package co.za.unicard.service;

import co.za.unicard.model.Learner;
import co.za.unicard.model.TxType;
import co.za.unicard.repository.MenuItemRepository;
import co.za.unicard.repository.WalletTransactionRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.util.Money;
import co.za.unicard.dto.LearnerDtos.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class LearnerService {

    /** Categories a parent can block in free-choice mode. */
    public static final Set<String> BLOCKABLE = Set.of("Fizzy drinks", "Sweets", "Snacks");

    private final LearnerAccess access;
    private final WalletService wallets;
    private final WalletTransactionRepository txs;
    private final MenuItemRepository menu;
    private final SchoolCalendar calendar;

    public LearnerService(LearnerAccess access, WalletService wallets, WalletTransactionRepository txs,
                          MenuItemRepository menu, SchoolCalendar calendar) {
        this.access = access;
        this.wallets = wallets;
        this.txs = txs;
        this.menu = menu;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public List<LearnerView> myLearners(Actor actor) {
        return access.myLearners(actor).stream().map(l -> LearnerView.of(l, calendar.today())).toList();
    }

    @Transactional(readOnly = true)
    public LearnerView get(Actor actor, Long id) {
        return LearnerView.of(access.require(actor, id), calendar.today());
    }

    public LearnerView updateControls(Actor actor, Long id, ControlsRequest req) {
        Learner l = access.requireAsParent(actor, id);
        if (req.dailyLimit() != null) l.setDailyLimit(Money.round(req.dailyLimit()));
        if (req.frozen() != null) l.setFrozen(req.frozen());
        if (req.tuckMode() != null) l.setTuckMode(req.tuckMode());
        if (req.blockedCategories() != null) {
            if (!BLOCKABLE.containsAll(req.blockedCategories())) {
                throw new IllegalArgumentException("Only these categories can be blocked: " + BLOCKABLE);
            }
            l.getBlockedCategories().clear();
            l.getBlockedCategories().addAll(req.blockedCategories());
        }
        if (req.allowedItems() != null) {
            Set<String> known = menu.findAll().stream().map(m -> m.getName()).collect(Collectors.toSet());
            if (!known.containsAll(req.allowedItems())) throw new IllegalArgumentException("Some items aren't on the tuckshop menu.");
            l.getAllowedItems().clear();
            l.getAllowedItems().addAll(req.allowedItems());
        }
        return LearnerView.of(l, calendar.today());
    }

    @Transactional(readOnly = true)
    public List<TxView> activity(Actor actor, Long id) {
        access.require(actor, id);
        return txs.forLearner(id, PageRequest.of(0, 30)).stream().map(TxView::of).toList();
    }

    @Transactional(readOnly = true)
    public CardView myCard(Actor actor) {
        Learner l = access.self(actor);
        return new CardView(LearnerView.of(l, calendar.today()), l.getCardToken());
    }

    /** Learner moves money from their card wallet into their savings goal (default R10). */
    public LearnerView saveTowardsGoal(Actor actor, BigDecimal amount) {
        Learner l = access.self(actor);
        if (l.getGoalName() == null) throw new IllegalArgumentException("Set up a savings goal first.");
        BigDecimal a = amount == null ? Money.of(10) : Money.round(amount);
        // A zero or negative amount would credit the card wallet and shrink the goal.
        if (!Money.isPositive(a)) throw new IllegalArgumentException("Choose an amount above R0.00.");
        wallets.debitLearner(l, a, TxType.GOAL_SAVING, "Saved towards " + l.getGoalName());
        l.addToGoal(a);
        return LearnerView.of(l, calendar.today());
    }
}
