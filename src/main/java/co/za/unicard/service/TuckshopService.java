package co.za.unicard.service;

import co.za.unicard.exception.ConflictException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.Learner;
import co.za.unicard.model.MenuItem;
import co.za.unicard.model.PreOrder;
import co.za.unicard.model.PreOrderLine;
import co.za.unicard.model.Role;
import co.za.unicard.model.TxType;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.MenuItemRepository;
import co.za.unicard.repository.PreOrderRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.util.Money;
import co.za.unicard.dto.TuckshopDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Parents and learners planning tuckshop food for the week. */
@Service
@Transactional
public class TuckshopService {

    private static final Logger log = LoggerFactory.getLogger(TuckshopService.class);
    public static final int ORDER_AHEAD_DAYS = 5;

    private final MenuItemRepository menu;
    private final PreOrderRepository orders;
    private final LearnerAccess access;
    private final WalletService wallets;
    private final SchoolCalendar calendar;

    public TuckshopService(MenuItemRepository menu, PreOrderRepository orders, LearnerAccess access,
                           WalletService wallets, SchoolCalendar calendar) {
        this.menu = menu;
        this.orders = orders;
        this.access = access;
        this.wallets = wallets;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public List<MenuItemView> menu() {
        return menu.findByAvailableTrueOrderByIdAsc().stream().map(MenuItemView::of).toList();
    }

    /** The next five school days for each of the actor's learners. */
    @Transactional(readOnly = true)
    public List<LearnerWeek> week(Actor actor) {
        List<LocalDate> days = calendar.upcomingSchoolDays(ORDER_AHEAD_DAYS);
        List<Learner> kids = access.myLearners(actor);
        if (kids.isEmpty()) return List.of();
        Map<String, PreOrder> byKey = new HashMap<>();
        orders.findForLearners(kids.stream().map(Learner::getId).toList(), days.get(0), days.get(days.size() - 1))
                .forEach(o -> byKey.put(o.getLearner().getId() + "|" + o.getDay(), o));
        LocalDate today = calendar.today();
        return kids.stream().map(k -> new LearnerWeek(k.getId(), k.getFirstName(), k.getWalletBalance(), k.getTuckMode(),
                days.stream().map(d -> {
                    PreOrder o = byKey.get(k.getId() + "|" + d);
                    return new DayView(d, d.equals(today), o == null ? null : PreOrderView.of(o));
                }).toList())).toList();
    }

    /**
     * Places an order, or replaces the existing one for that day. The old order is refunded to
     * whoever paid for it before the new one is charged; if the new charge fails the whole
     * thing rolls back.
     */
    public PreOrderView place(Actor actor, Long learnerId, PlaceOrderRequest req) {
        Learner learner = access.require(actor, learnerId);
        if (!calendar.upcomingSchoolDays(ORDER_AHEAD_DAYS).contains(req.day())) {
            throw new IllegalArgumentException("You can order for the next " + ORDER_AHEAD_DAYS + " school days.");
        }
        PreOrder.PayFrom payFrom = actor.is(Role.LEARNER) ? PreOrder.PayFrom.LEARNER
                : Objects.requireNonNullElse(req.payFrom(), PreOrder.PayFrom.PARENT);

        List<PreOrderLine> lines = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (OrderItem item : req.items()) {
            if (!seen.add(item.menuItemId())) throw new IllegalArgumentException("Each menu item can appear once; use quantity.");
            MenuItem m = menu.findById(item.menuItemId())
                    .filter(MenuItem::isAvailable)
                    .orElseThrow(() -> new NotFoundException("Menu item", item.menuItemId()));
            lines.add(new PreOrderLine(m.getName(), m.getCategory(), m.getPrice(), item.quantity()));
        }

        Optional<PreOrder> existing = orders.findByLearnerIdAndDay(learnerId, req.day());
        if (existing.isPresent()) {
            PreOrder ex = existing.get();
            if (ex.getStatus() != PreOrder.Status.WAITING) {
                throw new ConflictException("This order has already been collected, so it can't be changed.");
            }
            refund(ex, "Tuckshop order for " + learner.getFirstName() + " changed, refunded");
            orders.delete(ex);
            orders.flush();
        }

        BigDecimal total = lines.stream().map(PreOrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        String desc = "Tuckshop order for " + learner.getFirstName() + ", " + req.day();
        UserAccount payer = null;
        if (payFrom == PreOrder.PayFrom.PARENT) {
            payer = wallets.managed(actor.accountId());
            wallets.debitAccount(payer, total, TxType.TUCKSHOP_PREORDER, desc, null);
        } else {
            wallets.debitLearner(learner, total, TxType.TUCKSHOP_PREORDER, desc);
        }
        PreOrder saved = orders.save(new PreOrder(learner, req.day(), lines, payFrom, payer, req.repeatWeekly(), calendar.now()));
        return PreOrderView.of(saved);
    }

    public void cancel(Actor actor, Long learnerId, LocalDate day) {
        Learner learner = access.require(actor, learnerId);
        PreOrder o = orders.findByLearnerIdAndDay(learnerId, day)
                .orElseThrow(() -> new NotFoundException("There's no order for " + learner.getFirstName() + " on " + day));
        if (o.getStatus() != PreOrder.Status.WAITING) throw new ConflictException("This order has already been collected.");
        refund(o, "Tuckshop order cancelled, refunded");
        orders.delete(o);
    }

    private void refund(PreOrder o, String description) {
        if (o.getPayFrom() == PreOrder.PayFrom.PARENT && o.getPayer() != null) {
            wallets.creditAccount(wallets.managed(o.getPayer().getId()), o.getTotal(), TxType.TUCKSHOP_REFUND, description);
        } else {
            wallets.creditLearner(o.getLearner(), o.getTotal(), TxType.TUCKSHOP_REFUND, description);
        }
    }

    /**
     * Every school day at 18:00, orders marked "repeat weekly" are copied to the same day next
     * week, paid the same way. If there isn't enough money the copy is skipped.
     */
    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "${unischool.timezone:Africa/Johannesburg}")
    public void rollRepeatOrders() {
        LocalDate today = calendar.today();
        LocalDate next = today.plusWeeks(1);
        for (PreOrder o : orders.findByDayAndRepeatWeeklyTrue(today)) {
            Learner l = o.getLearner();
            if (orders.findByLearnerIdAndDay(l.getId(), next).isPresent()) continue;
            List<PreOrderLine> lines = o.getLines().stream().map(PreOrderLine::copy).toList();
            PreOrder copy = new PreOrder(l, next, lines, o.getPayFrom(), o.getPayer(), true, calendar.now());
            String desc = "Weekly tuckshop order for " + l.getFirstName() + ", " + next;
            // Check funds first: an exception thrown through the wallet service would mark the
            // whole transaction rollback-only, undoing every other copy made in this run.
            if (o.getPayFrom() == PreOrder.PayFrom.PARENT && o.getPayer() != null) {
                UserAccount payer = wallets.managed(o.getPayer().getId());
                if (Money.lessThan(payer.getWalletBalance(), copy.getTotal())) { skip(l, next); continue; }
                wallets.debitAccount(payer, copy.getTotal(), TxType.TUCKSHOP_PREORDER, desc, null);
            } else {
                if (Money.lessThan(l.getWalletBalance(), copy.getTotal())) { skip(l, next); continue; }
                wallets.debitLearner(l, copy.getTotal(), TxType.TUCKSHOP_PREORDER, desc);
            }
            orders.save(copy);
        }
    }

    private void skip(Learner l, LocalDate day) {
        log.info("Skipped repeat tuckshop order for learner {} on {}: not enough money", l.getId(), day);
    }
}
