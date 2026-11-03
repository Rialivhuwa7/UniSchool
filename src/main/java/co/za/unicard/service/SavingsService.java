package co.za.unicard.service;

import co.za.unicard.exception.ConflictException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.Learner;
import co.za.unicard.model.PocketMoneyRule;
import co.za.unicard.model.SavingsPot;
import co.za.unicard.model.TxType;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.PocketMoneyRuleRepository;
import co.za.unicard.repository.SavingsPotRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.util.Money;
import co.za.unicard.dto.SavingsDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SavingsService {

    private static final Logger log = LoggerFactory.getLogger(SavingsService.class);

    private final SavingsPotRepository pots;
    private final PocketMoneyRuleRepository pocket;
    private final WalletService wallets;
    private final LearnerAccess access;
    private final SchoolCalendar calendar;

    public SavingsService(SavingsPotRepository pots, PocketMoneyRuleRepository pocket, WalletService wallets,
                          LearnerAccess access, SchoolCalendar calendar) {
        this.pots = pots;
        this.pocket = pocket;
        this.wallets = wallets;
        this.access = access;
        this.calendar = calendar;
    }

    public PotView pot(Actor actor) {
        return view(potFor(actor));
    }

    /** Moves money from the parent's wallet into the pot now. */
    public PotView addNow(Actor actor, BigDecimal amount) {
        SavingsPot pot = potFor(actor);
        UserAccount a = wallets.managed(actor.accountId());
        BigDecimal amt = Money.round(amount);
        wallets.debitAccount(a, amt, TxType.SAVINGS, "To savings pot", "Not enough in your wallet.");
        pot.contribute("You", amt, calendar.today());
        return view(pot);
    }

    public PotView setMonthly(Actor actor, BigDecimal monthly) {
        SavingsPot pot = potFor(actor);
        pot.setMonthlyDebit(Money.round(monthly));
        return view(pot);
    }

    // ---- family share link (public) ----
    @Transactional(readOnly = true)
    public GivePage givePage(String token) {
        SavingsPot pot = pots.findByShareToken(token).orElseThrow(() -> new NotFoundException("This link isn't valid any more."));
        return new GivePage("the " + pot.getAccount().getFullName().replaceAll(".* ", "") + " family",
                "Help get the children ready for school. Your gift goes into their back-to-school pot.");
    }

    /**
     * A relative adds to the pot. In production this is called after the card payment
     * succeeds at the payment provider; relatives never see anything else in the account.
     */
    public void give(String token, GiveRequest req) {
        SavingsPot pot = pots.findByShareToken(token).orElseThrow(() -> new NotFoundException("This link isn't valid any more."));
        pot.contribute(req.fromName().trim(), Money.round(req.amount()), calendar.today());
    }

    // ---- pocket money ----
    @Transactional(readOnly = true)
    public List<PocketMoneyView> pocketMoney(Actor actor) {
        return pocket.findByAccount(actor.accountId()).stream().map(this::view).toList();
    }

    public PocketMoneyView addPocketMoney(Actor actor, PocketMoneyRequest req) {
        Learner l = access.requireAsParent(actor, req.learnerId());
        boolean exists = pocket.findByAccount(actor.accountId()).stream().anyMatch(r -> r.getLearner().getId().equals(l.getId()));
        if (exists) throw new ConflictException(l.getFirstName() + " already has pocket money set up. Change it instead.");
        PocketMoneyRule r = new PocketMoneyRule(wallets.managed(actor.accountId()), l, Money.round(req.amount()),
                req.dayOfWeek() == null ? java.time.DayOfWeek.MONDAY : req.dayOfWeek(),
                req.enabled() == null || req.enabled());
        return view(pocket.save(r));
    }

    public PocketMoneyView updatePocketMoney(Actor actor, Long ruleId, PocketMoneyUpdate req) {
        PocketMoneyRule r = pocket.findById(ruleId)
                .filter(x -> x.getAccount().getId().equals(actor.accountId()))
                .orElseThrow(() -> new NotFoundException("Pocket money rule", ruleId));
        if (req.amount() != null) r.setAmount(Money.round(req.amount()));
        if (req.dayOfWeek() != null) r.setDayOfWeek(req.dayOfWeek());
        if (req.enabled() != null) r.setEnabled(req.enabled());
        return view(r);
    }

    /** Pays pocket money at 07:00 on each rule's day. Skips (and logs) if the parent's wallet is short. */
    @Scheduled(cron = "0 0 7 * * *", zone = "${unischool.timezone:Africa/Johannesburg}")
    public void payPocketMoney() {
        LocalDate today = calendar.today();
        for (PocketMoneyRule r : pocket.findByEnabledTrueAndDayOfWeek(today.getDayOfWeek())) {
            UserAccount parent = wallets.managed(r.getAccount().getId());
            if (Money.lessThan(parent.getWalletBalance(), r.getAmount())) {
                log.info("Pocket money skipped for learner {}: parent wallet too low", r.getLearner().getId());
                continue;
            }
            wallets.debitAccount(parent, r.getAmount(), TxType.POCKET_MONEY, "Pocket money for " + r.getLearner().getFirstName(), null);
            wallets.creditLearner(r.getLearner(), r.getAmount(), TxType.POCKET_MONEY, "Pocket money");
        }
    }

    private SavingsPot potFor(Actor actor) {
        return pots.findByAccountId(actor.accountId())
                .orElseGet(() -> pots.save(new SavingsPot(wallets.managed(actor.accountId()), Money.ZERO, Money.of(5000),
                        Money.of(500), LocalDate.of(calendar.today().getYear() + 1, 1, 2))));
    }

    private PotView view(SavingsPot p) {
        LocalDate today = calendar.today();
        BigDecimal projected = p.projected(today);
        boolean onTrack = projected.compareTo(p.getGoal()) >= 0;
        return new PotView(p.getBalance(), p.getGoal(), p.getMonthlyDebit(), p.debitsRemaining(today), projected, onTrack,
                onTrack ? Money.ZERO : p.getGoal().subtract(projected), p.getOpensOn(),
                "/api/give/" + p.getShareToken(),
                p.getContributions().stream().map(c -> new ContributionView(c.getFromName(), c.getAmount(), c.getContributedOn())).toList());
    }

    private PocketMoneyView view(PocketMoneyRule r) {
        return new PocketMoneyView(r.getId(), r.getLearner().getId(), r.getLearner().getFirstName(), r.getAmount(),
                r.getDayOfWeek(), r.isEnabled());
    }
}
