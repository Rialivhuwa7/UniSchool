package co.za.unicard.service;

import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.Learner;
import co.za.unicard.model.MenuItem;
import co.za.unicard.model.PreOrder;
import co.za.unicard.model.PreOrderLine;
import co.za.unicard.model.TuckMode;
import co.za.unicard.model.TxType;
import co.za.unicard.repository.LearnerRepository;
import co.za.unicard.repository.MenuItemRepository;
import co.za.unicard.repository.PreOrderRepository;
import co.za.unicard.repository.WalletTransactionRepository;
import co.za.unicard.util.Money;
import co.za.unicard.dto.TuckshopDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * The tuckshop till. A learner taps their card (or the operator searches by name). Purchases
 * are checked against the parent's rules in this order: frozen card, pre-order-only mode,
 * approved items, blocked categories, daily limit, wallet balance. A declined sale charges
 * nothing and returns a message the operator can read out.
 */
@Service
@Transactional
public class PosService {

    private final LearnerRepository learners;
    private final MenuItemRepository menu;
    private final PreOrderRepository orders;
    private final WalletService wallets;
    private final WalletTransactionRepository txs;
    private final SchoolCalendar calendar;

    public PosService(LearnerRepository learners, MenuItemRepository menu, PreOrderRepository orders,
                      WalletService wallets, WalletTransactionRepository txs, SchoolCalendar calendar) {
        this.learners = learners;
        this.menu = menu;
        this.orders = orders;
        this.wallets = wallets;
        this.txs = txs;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public List<TillLearnerView> search(String q) {
        if (q == null || q.isBlank()) return List.of();
        return learners.search(q.trim()).stream().limit(20).map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public TillLearnerView learner(Long id) {
        return view(learners.findById(id).orElseThrow(() -> new NotFoundException("Learner", id)));
    }

    @Transactional(readOnly = true)
    public TillLearnerView byCard(String token) {
        return view(learners.findByCardToken(token).orElseThrow(() -> new NotFoundException("Card not recognised")));
    }

    public ChargeResult charge(ChargeRequest req) {
        Learner k = resolve(req);
        LocalDate today = calendar.today();
        List<MenuItem> items = new ArrayList<>();
        for (Long id : req.menuItemIds()) {
            items.add(menu.findById(id).filter(MenuItem::isAvailable).orElseThrow(() -> new NotFoundException("Menu item", id)));
        }
        BigDecimal total = items.stream().map(MenuItem::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        String name = k.getFirstName();

        if (k.isFrozen()) {
            return declined("Declined. " + name + "'s card is frozen by a parent.", total, k);
        }
        if (k.getTuckMode() == TuckMode.PREORDER_ONLY) {
            return declined("Declined. " + name + " can only collect what their parent pre-orders. Nothing was charged.", total, k);
        }
        if (k.getTuckMode() == TuckMode.APPROVED_ITEMS) {
            Optional<MenuItem> na = items.stream().filter(i -> !k.getAllowedItems().contains(i.getName())).findFirst();
            if (na.isPresent()) {
                return declined("Declined. " + na.get().getName() + " isn't on " + name + "'s approved list. Remove it to continue.", total, k);
            }
        }
        if (k.getTuckMode() == TuckMode.FREE_CHOICE) {
            Optional<MenuItem> bad = items.stream().filter(i -> k.getBlockedCategories().contains(i.getCategory())).findFirst();
            if (bad.isPresent()) {
                return declined("Declined. " + bad.get().getCategory() + " are blocked by " + name + "'s parent. Remove "
                        + bad.get().getName() + ".", total, k);
            }
        }
        if (k.spentOn(today).add(total).compareTo(k.getDailyLimit()) > 0) {
            return declined("Declined. Over the daily limit: " + Money.format(k.remainingToday(today)) + " left today.", total, k);
        }
        if (Money.lessThan(k.getWalletBalance(), total)) {
            return declined("Declined. Only " + Money.format(k.getWalletBalance()) + " in the wallet.", total, k);
        }

        String what = String.join(", ", items.stream().map(i -> i.getName().toLowerCase()).toList());
        wallets.debitLearner(k, total, TxType.TUCKSHOP_SALE, "Tuckshop: " + what);
        k.recordSpend(total, today);
        return new ChargeResult(true, "Approved. " + Money.format(total) + " paid. "
                + Money.format(k.getWalletBalance()) + " left in the wallet.", total, k.getWalletBalance());
    }

    /** Hands over some or all of a pre-order. */
    public HandoutResult handout(Long orderId, HandoutRequest req) {
        PreOrder o = orders.findById(orderId).orElseThrow(() -> new NotFoundException("Pre-order", orderId));
        if (!o.getDay().equals(calendar.serviceDay())) {
            return new HandoutResult(false, "This order is for " + o.getDay() + ", not today.", PreOrderView.of(o));
        }
        Set<Integer> wanted = req == null || req.lineIndexes() == null || req.lineIndexes().isEmpty()
                ? null : new HashSet<>(req.lineIndexes());
        LocalTime at = calendar.timeNow().withSecond(0).withNano(0);
        List<String> taken = new ArrayList<>();
        for (int i = 0; i < o.getLines().size(); i++) {
            PreOrderLine line = o.getLines().get(i);
            if (!line.isCollected() && (wanted == null || wanted.contains(i))) {
                line.markCollected(at);
                taken.add(line.getItemName());
            }
        }
        if (taken.isEmpty()) return new HandoutResult(false, "Tick at least one item to hand over.", PreOrderView.of(o));
        o.refreshStatus(at);
        List<String> left = o.getLines().stream().filter(l -> !l.isCollected()).map(PreOrderLine::getItemName).toList();
        wallets.noteLearner(o.getLearner(), TxType.TUCKSHOP_COLLECTED, "Collected at tuckshop: "
                + String.join(", ", taken).toLowerCase() + (left.isEmpty() ? "" : " (" + left.size() + " still to collect)"));
        String msg = "Handed over: " + String.join(", ", taken) + "."
                + (left.isEmpty() ? "" : " Still to collect later: " + String.join(", ", left) + ".");
        return new HandoutResult(true, msg, PreOrderView.of(o));
    }

    @Transactional(readOnly = true)
    public TillSummary today() {
        LocalDate day = calendar.serviceDay();
        List<PreOrder> list = orders.findByDayWithLearner(day);
        BigDecimal preorderValue = list.stream().map(PreOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tillSales = txs.totalSpentSince(TxType.TUCKSHOP_SALE, calendar.today().atStartOfDay());
        long tillCount = txs.countSince(TxType.TUCKSHOP_SALE, calendar.today().atStartOfDay());
        return new TillSummary(day, list.size(),
                list.stream().filter(o -> o.getStatus() == PreOrder.Status.COLLECTED).count(),
                Money.round(tillSales.add(preorderValue)), tillCount,
                list.stream().map(PreOrderView::of).toList());
    }

    private Learner resolve(ChargeRequest req) {
        if (req.cardToken() != null && !req.cardToken().isBlank()) {
            return learners.findByCardToken(req.cardToken()).orElseThrow(() -> new NotFoundException("Card not recognised"));
        }
        if (req.learnerId() == null) throw new IllegalArgumentException("Tap a card or choose a learner.");
        return learners.findById(req.learnerId()).orElseThrow(() -> new NotFoundException("Learner", req.learnerId()));
    }

    private ChargeResult declined(String message, BigDecimal total, Learner k) {
        return new ChargeResult(false, message, total, k.getWalletBalance());
    }

    private TillLearnerView view(Learner k) {
        LocalDate today = calendar.today();
        PreOrderView todays = orders.findByLearnerIdAndDay(k.getId(), calendar.serviceDay()).map(PreOrderView::of).orElse(null);
        return new TillLearnerView(k.getId(), k.getShortName(), k.getGrade(), k.getCampus().displayName(), k.getWalletBalance(),
                k.remainingToday(today), k.getTuckMode(), k.isFrozen(), todays);
    }
}
