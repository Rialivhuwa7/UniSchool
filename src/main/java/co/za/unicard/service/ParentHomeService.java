package co.za.unicard.service;

import co.za.unicard.model.Learner;
import co.za.unicard.model.ProductCategory;
import co.za.unicard.model.ShopOrder;
import co.za.unicard.repository.FeeAccountRepository;
import co.za.unicard.repository.SavingsPotRepository;
import co.za.unicard.repository.ShopOrderRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.dto.SchoolDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** The parent's home screen: wallet, unread notices and the "ready for next year" checklist. */
@Service
@Transactional(readOnly = true)
public class ParentHomeService {

    private final LearnerAccess access;
    private final FeeAccountRepository fees;
    private final PaymentService payments;
    private final ShopOrderRepository orders;
    private final SavingsPotRepository pots;
    private final NoticeService notices;
    private final WalletService wallets;
    private final SchoolCalendar calendar;

    public ParentHomeService(LearnerAccess access, FeeAccountRepository fees, PaymentService payments, ShopOrderRepository orders,
                             SavingsPotRepository pots, NoticeService notices, WalletService wallets, SchoolCalendar calendar) {
        this.access = access;
        this.fees = fees;
        this.payments = payments;
        this.orders = orders;
        this.pots = pots;
        this.notices = notices;
        this.wallets = wallets;
        this.calendar = calendar;
    }

    public ParentHome home(Actor actor) {
        List<Learner> kids = access.myLearners(actor);
        List<ShopOrder> mine = orders.findByAccountIdOrderByPlacedAtDesc(actor.accountId());
        int year = payments.feeYear();
        List<ReadinessItem> items = new ArrayList<>();

        boolean feesSet = !kids.isEmpty() && kids.stream().allMatch(k ->
                fees.findByLearnerIdAndFeeYear(k.getId(), year).map(f -> f.getPlan() != null).orElse(false));
        items.add(new ReadinessItem(year + " fee plan chosen for " + (kids.size() > 1 ? "all children" : kids.isEmpty() ? "your child" : kids.get(0).getFirstName()),
                feesSet, "fees", null));
        for (Learner k : kids) {
            boolean ordered = mine.stream().anyMatch(o -> o.getLines().stream()
                    .anyMatch(l -> l.getCategory() == ProductCategory.SUPPLY_LIST && l.getLearner().getId().equals(k.getId())));
            items.add(new ReadinessItem(k.getFirstName() + "'s " + k.getGrade() + " list ordered", ordered, "supply-list", k.getId()));
        }
        boolean uniform = mine.stream().anyMatch(o -> o.getLines().stream().anyMatch(l -> l.getCategory() == ProductCategory.UNIFORM));
        items.add(new ReadinessItem("Uniform sizes checked and ordered", uniform, "uniform", null));
        boolean savings = pots.findByAccountId(actor.accountId())
                .map(p -> p.projected(calendar.today()).compareTo(p.getGoal()) >= 0).orElse(false);
        items.add(new ReadinessItem("Savings pot on track for January", savings, "savings", null));
        boolean sport = mine.stream().anyMatch(o -> o.getLines().stream().anyMatch(l -> l.getCategory() == ProductCategory.SPORT));
        items.add(new ReadinessItem("Sport kit sorted", sport, "sport", null));

        return new ParentHome(actor.account().getFirstName(), wallets.managed(actor.accountId()).getWalletBalance(),
                notices.unreadCount(actor), items.stream().filter(ReadinessItem::done).count(), items);
    }
}
