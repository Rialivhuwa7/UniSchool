package co.za.unicard.service;

import co.za.unicard.exception.ConflictException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.CollectionPayment;
import co.za.unicard.model.FeeAccount;
import co.za.unicard.model.FeePlan;
import co.za.unicard.model.Learner;
import co.za.unicard.model.LedgerSource;
import co.za.unicard.model.SchoolCollection;
import co.za.unicard.model.SchoolLedgerEntry;
import co.za.unicard.model.Ticket;
import co.za.unicard.model.TxType;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.CollectionPaymentRepository;
import co.za.unicard.repository.FeeAccountRepository;
import co.za.unicard.repository.SchoolCollectionRepository;
import co.za.unicard.repository.SchoolLedgerRepository;
import co.za.unicard.repository.TicketRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.util.Audience;
import co.za.unicard.util.Money;
import co.za.unicard.dto.PaymentDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Parents paying the school: trips, events and levies, tickets, donations and fee plans. */
@Service
@Transactional
public class PaymentService {

    private final SchoolCollectionRepository collections;
    private final CollectionPaymentRepository payments;
    private final TicketRepository tickets;
    private final FeeAccountRepository fees;
    private final SchoolLedgerRepository ledger;
    private final LearnerAccess access;
    private final WalletService wallets;
    private final SchoolCalendar calendar;

    public PaymentService(SchoolCollectionRepository collections, CollectionPaymentRepository payments, TicketRepository tickets,
                          FeeAccountRepository fees, SchoolLedgerRepository ledger, LearnerAccess access,
                          WalletService wallets, SchoolCalendar calendar) {
        this.collections = collections;
        this.payments = payments;
        this.tickets = tickets;
        this.fees = fees;
        this.ledger = ledger;
        this.access = access;
        this.wallets = wallets;
        this.calendar = calendar;
    }

    /** Every collection that applies to each of the parent's children, with what's paid and owing. */
    @Transactional(readOnly = true)
    public List<CollectionForChild> collections(Actor actor) {
        List<CollectionForChild> out = new ArrayList<>();
        List<SchoolCollection> all = collections.findAllByOrderByCreatedAtDesc();
        for (Learner l : access.myLearners(actor)) {
            for (SchoolCollection k : all) {
                if (!Audience.matches(l, k.getCampus(), k.getGrades(), List.of())) continue;
                out.add(view(k, l));
            }
        }
        return out;
    }

    public PaymentReceipt pay(Actor actor, Long collectionId, PayRequest req) {
        Learner l = access.requireAsParent(actor, req.learnerId());
        SchoolCollection k = collections.findById(collectionId).orElseThrow(() -> new NotFoundException("Collection", collectionId));
        if (!Audience.matches(l, k.getCampus(), k.getGrades(), List.of())) {
            throw new IllegalArgumentException(k.getTitle() + " isn't for " + l.getFirstName() + "'s grade.");
        }
        BigDecimal amount;
        String label;
        int qty = 0;
        switch (k.getKind()) {
            case PAYMENT -> {
                BigDecimal owing = owing(k, l);
                if (owing.signum() == 0) throw new ConflictException(k.getTitle() + " is already paid for " + l.getFirstName() + ".");
                boolean instalment = req.part() == PayPart.INSTALMENT && k.getInstalments() > 1;
                amount = instalment ? Money.divide(k.getAmount(), k.getInstalments()).min(owing) : owing;
                label = k.getTitle() + (instalment ? " (instalment)" : "");
            }
            case TICKET -> {
                qty = req.quantity() == null ? 1 : req.quantity();
                amount = Money.round(k.getAmount().multiply(BigDecimal.valueOf(qty)));
                label = k.getTitle() + ", " + qty + " ticket" + (qty > 1 ? "s" : "");
            }
            case DONATION -> {
                amount = Money.round(req.amount() == null ? k.getAmount() : req.amount());
                label = k.getTitle() + ", donation";
            }
            default -> throw new IllegalStateException();
        }

        UserAccount payer = wallets.managed(actor.accountId());
        CollectionPayment.Method method = req.method() == null ? CollectionPayment.Method.WALLET : req.method();
        if (method == CollectionPayment.Method.WALLET) {
            wallets.debitAccount(payer, amount, TxType.SCHOOL_PAYMENT, label,
                    "Your wallet has " + Money.format(payer.getWalletBalance()) + ". Add money or pay by card.");
        }
        payments.save(new CollectionPayment(k, l, payer, amount, qty, method, calendar.now()));
        ledger.save(new SchoolLedgerEntry(calendar.now(), label, amount, initialAndSurname(payer), LedgerSource.COLLECTION));

        List<String> codes = new ArrayList<>();
        for (int i = 0; i < qty; i++) {
            Ticket t = tickets.saveAndFlush(new Ticket(k, payer, calendar.now()));
            t.assignCode();
            codes.add(t.getCode());
        }
        return new PaymentReceipt("Paid " + Money.format(amount) + ". The school can see it now.", amount, codes);
    }

    @Transactional(readOnly = true)
    public List<TicketView> tickets(Actor actor) {
        return tickets.findByOwner(actor.accountId()).stream()
                .map(t -> new TicketView(t.getCode(), t.getCollection().getTitle(), t.getCollection().getId())).toList();
    }

    // ---- fees ----
    public int feeYear() { return calendar.today().getYear() + 1; }

    @Transactional(readOnly = true)
    public List<FeeView> fees(Actor actor) {
        int year = feeYear();
        return access.myLearners(actor).stream()
                .map(l -> fees.findByLearnerIdAndFeeYear(l.getId(), year).map(this::view).orElse(null))
                .filter(java.util.Objects::nonNull).toList();
    }

    /** Saves the plan. The parent then approves the debit order in their banking app. */
    public FeeView choosePlan(Actor actor, Long learnerId, FeePlan plan) {
        Learner l = access.requireAsParent(actor, learnerId);
        FeeAccount f = fees.findByLearnerIdAndFeeYear(l.getId(), feeYear())
                .orElseThrow(() -> new NotFoundException("The school hasn't loaded " + feeYear() + " fees for " + l.getFirstName() + " yet."));
        f.choosePlan(plan, calendar.now());
        return view(f);
    }

    FeeView view(FeeAccount f) {
        BigDecimal annual = f.getAnnualFee();
        List<PlanOption> options = Arrays.stream(FeePlan.values())
                .map(p -> new PlanOption(p, p.title(), p.description(), p.payments(), p.instalment(annual), p.totalCost(annual))).toList();
        FeePlan p = f.getPlan();
        Learner l = f.getLearner();
        return new FeeView(l.getId(), l.getFirstName(), l.getCampus().displayName(), f.getFeeYear(), annual, p,
                p == null ? null : p.title(), p == null ? null : p.instalment(annual), p == null ? 0 : p.payments(),
                LocalDate.of(f.getFeeYear(), 1, 2), f.isPreviousYearPaid(), options);
    }

    // ---- helpers ----
    BigDecimal paid(SchoolCollection k, Learner l) {
        return payments.findByCollectionIdAndLearnerId(k.getId(), l.getId()).stream()
                .map(CollectionPayment::getAmount).reduce(Money.ZERO, BigDecimal::add);
    }

    BigDecimal owing(SchoolCollection k, Learner l) {
        if (k.getKind() != SchoolCollection.Kind.PAYMENT) return Money.ZERO;
        return k.getAmount().subtract(paid(k, l)).max(Money.ZERO);
    }

    private CollectionForChild view(SchoolCollection k, Learner l) {
        List<CollectionPayment> ps = payments.findByCollectionIdAndLearnerId(k.getId(), l.getId());
        BigDecimal paid = ps.stream().map(CollectionPayment::getAmount).reduce(Money.ZERO, BigDecimal::add);
        int ticketCount = ps.stream().mapToInt(CollectionPayment::getTickets).sum();
        BigDecimal owing = k.getKind() == SchoolCollection.Kind.PAYMENT ? k.getAmount().subtract(paid).max(Money.ZERO) : Money.ZERO;
        String status = switch (k.getKind()) {
            case TICKET -> ticketCount > 0 ? ticketCount + " ticket" + (ticketCount > 1 ? "s" : "") : "Not bought";
            case DONATION -> paid.signum() > 0 ? "Gave " + Money.format(paid) : "Optional";
            case PAYMENT -> owing.signum() == 0 ? "Paid" : paid.signum() > 0 ? Money.format(paid) + " paid" : "Not paid";
        };
        return new CollectionForChild(k.getId(), l.getId(), l.getFirstName(), k.getTitle(), k.getKind(), k.getAmount(),
                k.getDueDate(), k.getInstalments(), paid, owing, ticketCount, status);
    }

    static String initialAndSurname(UserAccount a) {
        String[] parts = a.getFullName().split(" ");
        return parts.length > 1 ? parts[0].charAt(0) + ". " + parts[parts.length - 1] : a.getFullName();
    }
}
