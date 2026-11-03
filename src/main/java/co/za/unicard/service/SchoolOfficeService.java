package co.za.unicard.service;

import co.za.unicard.exception.ConflictException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.AccessRequest;
import co.za.unicard.model.Campus;
import co.za.unicard.model.FeePlan;
import co.za.unicard.model.Learner;
import co.za.unicard.model.LedgerSource;
import co.za.unicard.model.RequestedLearner;
import co.za.unicard.model.Role;
import co.za.unicard.model.SchoolLedgerEntry;
import co.za.unicard.model.SupplyList;
import co.za.unicard.model.SupplyListItem;
import co.za.unicard.model.UniformStock;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.AccessRequestRepository;
import co.za.unicard.repository.FeeAccountRepository;
import co.za.unicard.repository.LearnerRepository;
import co.za.unicard.repository.SchoolLedgerRepository;
import co.za.unicard.repository.ShopOrderRepository;
import co.za.unicard.repository.SupplyListRepository;
import co.za.unicard.repository.UniformStockRepository;
import co.za.unicard.repository.UserAccountRepository;
import co.za.unicard.util.Money;
import co.za.unicard.dto.SchoolDtos.*;
import co.za.unicard.dto.SetupDtos.TeacherAssignment;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class SchoolOfficeService {

    /** Grades shown in the supply-list overview. */
    public static final Map<Campus, List<String>> GRADES = Map.of(
            Campus.PREP, List.of("Grade R", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6", "Grade 7"),
            Campus.COLLEGE, List.of("Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"));

    private final CollectionAdminService collections;
    private final SchoolLedgerRepository ledger;
    private final FeeAccountRepository fees;
    private final LearnerRepository learners;
    private final AccessRequestRepository requests;
    private final UserAccountRepository accounts;
    private final SupplyListRepository lists;
    private final ShopOrderRepository orders;
    private final UniformStockRepository stock;
    private final SchoolCalendar calendar;

    public SchoolOfficeService(CollectionAdminService collections, SchoolLedgerRepository ledger, FeeAccountRepository fees,
                               LearnerRepository learners, AccessRequestRepository requests, UserAccountRepository accounts,
                               SupplyListRepository lists, ShopOrderRepository orders, UniformStockRepository stock,
                               SchoolCalendar calendar) {
        this.collections = collections;
        this.ledger = ledger;
        this.fees = fees;
        this.learners = learners;
        this.requests = requests;
        this.accounts = accounts;
        this.lists = lists;
        this.orders = orders;
        this.stock = stock;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        FeePlanBreakdown f = feePlans();
        long withPlan = f.total() - f.withoutPlan();
        BigDecimal giveBack = ledger.totalSince(LedgerSource.SHOP_GIVEBACK, LocalDate.of(calendar.today().getYear(), 1, 1).atStartOfDay());
        List<FeedItem> feed = ledger.latestPayments(PageRequest.of(0, 8)).stream()
                .map(e -> new FeedItem(e.getAt(), e.getDescription(), e.getAmount(), e.getPayerName())).toList();
        return new Overview(collections.totalCollected(), withPlan, f.total(), Money.pct(withPlan, f.total()),
                Money.round(giveBack), Money.ZERO, collections.all(), feed);
    }

    // ---- access ----
    @Transactional(readOnly = true)
    public List<AccessRequestView> pendingAccess() {
        return requests.findByStatusOrderByCreatedAtAsc(AccessRequest.Status.PENDING).stream()
                .map(r -> new AccessRequestView(r.getId(), r.getAccount().getFullName(), r.getAccount().getUsername(),
                        r.getAsk(), r.getHow(), r.getRequestedRole().name(), r.getCreatedAt(), namedLearners(r))).toList();
    }

    private List<String> namedLearners(AccessRequest r) {
        List<String> out = new ArrayList<>();
        for (Long id : r.getLearnerIds()) {
            learners.findById(id).ifPresent(l -> out.add(l.getFullName() + ", " + l.getGrade() + ", " + l.getCampus().displayName()
                    + " (in UniSchool, " + l.getStudentNumber() + ")"));
        }
        for (RequestedLearner n : r.getNewLearners()) {
            out.add(n.getFirstName() + " " + n.getLastName() + ", " + n.getGrade() + ", " + n.getCampus().displayName()
                    + " (new" + (n.getStudentNumber() == null ? "" : ", student number " + n.getStudentNumber()) + ")");
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<StaffView> staff() {
        return accounts.findVerifiedStaffRoles().stream()
                .map(r -> new StaffView(r.getAccount().getFullName(), r.getLabel(),
                        r.getVerifiedNote() == null ? "Verified" : r.getVerifiedNote())).toList();
    }

    /** Verifies the person's role; parents are linked to the children they named. */
    /**
     * Lets the person sign in with the role they asked for. A teacher's grade and class must be
     * given here: their class list, homework and groups all hang off it.
     */
    public Message approveAccess(Long id, TeacherAssignment teaching) {
        AccessRequest r = pending(id);
        UserAccount a = r.getAccount();
        if (r.getRequestedRole() == Role.TEACHER) {
            if (teaching == null || teaching.campus() == null || teaching.grade() == null || teaching.grade().isBlank()) {
                throw new IllegalArgumentException("Say which school and grade this teacher teaches.");
            }
            if (teaching.campus() == Campus.BOTH) throw new IllegalArgumentException("Choose Prep or College for a teacher.");
            String className = teaching.className() == null || teaching.className().isBlank() ? null : teaching.className().trim();
            a.role(Role.TEACHER).ifPresent(role -> role.assignTeaching(
                    teaching.grade() + " teacher, " + teaching.campus().displayName(), teaching.campus(), teaching.grade(), className));
        }
        a.role(r.getRequestedRole()).ifPresent(role -> role.verify("Verified " + calendar.today()));
        List<Learner> theirLearners = new ArrayList<>();
        for (Long learnerId : r.getLearnerIds()) {
            theirLearners.add(learners.findById(learnerId).orElseThrow(() -> new NotFoundException("Learner", learnerId)));
        }
        for (RequestedLearner n : r.getNewLearners()) theirLearners.add(enrol(n));
        if (r.getRequestedRole() == Role.PARENT) {
            for (Learner l : theirLearners) l.getGuardians().add(a);
        }
        if (r.getRequestedRole() == Role.LEARNER && !theirLearners.isEmpty()) {
            Learner self = theirLearners.get(0);
            if (accounts.findLearnerSignIn(self.getId()).isPresent()) {
                throw new ConflictException(self.getFullName() + " already has a sign-in. Decline this request instead.");
            }
            a.role(Role.LEARNER).ifPresent(role -> role.linkLearner(self));
        }
        r.decide(AccessRequest.Status.APPROVED, calendar.now());
        long created = r.getNewLearners().size();
        return new Message(a.getFullName() + " approved. They can sign in now."
                + (created == 0 ? "" : " " + created + " new learner" + (created == 1 ? "" : "s") + " added to the enrolment record."));
    }

    /** A learner named in a request: use the enrolment record if the student number is there by now, else create them. */
    private Learner enrol(RequestedLearner n) {
        if (n.getStudentNumber() != null) {
            Optional<Learner> existing = learners.findByStudentNumber(n.getStudentNumber());
            if (existing.isPresent()) return existing.get();
        }
        String number = n.getStudentNumber() != null ? n.getStudentNumber() : newStudentNumber();
        return learners.save(new Learner(number, n.getFirstName(), n.getLastName(), n.getGrade(), n.getCampus(), null));
    }

    /** A student number for a learner the parent didn't give one for, e.g. "U2026-4821". The office can't change it later. */
    private String newStudentNumber() {
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        String number;
        do {
            number = "U" + calendar.today().getYear() + "-" + (1000 + random.nextInt(9000));
        } while (learners.findByStudentNumber(number).isPresent());
        return number;
    }

    public Message declineAccess(Long id) {
        AccessRequest r = pending(id);
        r.decide(AccessRequest.Status.DECLINED, calendar.now());
        r.getAccount().setActive(false);
        return new Message("Request from " + r.getAccount().getFullName() + " declined.");
    }

    // ---- fees ----
    @Transactional(readOnly = true)
    public FeePlanBreakdown feePlans() {
        int year = calendar.today().getYear() + 1;
        Map<String, Long> by = new LinkedHashMap<>();
        for (FeePlan p : FeePlan.values()) by.put(p.name(), 0L);
        long none = 0, total = 0;
        for (Object[] row : fees.planCounts(year)) {
            long n = (Long) row[1];
            total += n;
            if (row[0] == null) none += n;
            else by.put(((FeePlan) row[0]).name(), n);
        }
        return new FeePlanBreakdown(year, by, none, total);
    }

    @Transactional(readOnly = true)
    public Message nudgeFees() {
        long n = feePlans().withoutPlan();
        return new Message("Reminder sent to " + n + " families without a " + (calendar.today().getYear() + 1) + " fee plan.");
    }

    // ---- supply lists ----
    @Transactional(readOnly = true)
    public List<ListStatus> listStatus() {
        Map<String, Long> ordered = new HashMap<>();
        for (Object[] row : orders.supplyListOrdersByGrade()) ordered.put(row[1] + "|" + row[0], (Long) row[2]);
        List<ListStatus> out = new ArrayList<>();
        for (Campus c : List.of(Campus.PREP, Campus.COLLEGE)) {
            for (String g : GRADES.get(c)) {
                Optional<SupplyList> l = lists.findByCampusAndGrade(c, g);
                String status = l.map(x -> x.getStatus() == SupplyList.Status.PUBLISHED ? "Published" : "Draft").orElse("Not uploaded");
                out.add(new ListStatus(c, g, status, ordered.getOrDefault(c + "|" + g, 0L), l.map(x -> x.getItems().size()).orElse(0)));
            }
        }
        return out;
    }

    public ListStatus saveList(Campus campus, String grade, PublishListRequest req) {
        if (campus == Campus.BOTH) throw new IllegalArgumentException("Choose Prep or College.");
        SupplyList list = lists.findByCampusAndGrade(campus, grade).orElseGet(() -> lists.save(new SupplyList(campus, grade)));
        list.replaceItems(req.items().stream()
                .map(i -> new SupplyListItem(i.name().trim(), i.quantity(), Money.round(i.unitPrice()))).toList());
        list.setStatus(req.publish() ? SupplyList.Status.PUBLISHED : SupplyList.Status.DRAFT);
        return listStatus().stream().filter(s -> s.campus() == campus && s.grade().equals(grade)).findFirst()
                .orElse(new ListStatus(campus, grade, req.publish() ? "Published" : "Draft", 0, req.items().size()));
    }

    // ---- uniform ----
    @Transactional(readOnly = true)
    public UniformView uniform() {
        List<StockLine> lines = stock.findAllByOrderByItemNameAscSizeAsc().stream()
                .map(s -> new StockLine(s.getId(), s.getItemName(), s.getSize(), s.getQuantity(), s.isLow(), s.isRestockRequested())).toList();
        return new UniformView(Money.round(orders.uniformSales()), lines.stream().filter(StockLine::low).count(), lines);
    }

    /** Asks the supplier to restock. Ordering from suppliers isn't integrated yet; this flags the line. */
    public Message requestRestock(Long stockId) {
        UniformStock s = stock.findById(stockId).orElseThrow(() -> new NotFoundException("Stock line", stockId));
        if (s.isRestockRequested()) throw new ConflictException("Restock already requested for " + s.getItemName() + " size " + s.getSize() + ".");
        s.setRestockRequested(true);
        return new Message("Restock requested: " + s.getItemName() + ", size " + s.getSize() + ".");
    }

    // ---- payouts ----
    /** Money owed to the school grouped by day and source. Today's is pending; earlier days have settled. */
    @Transactional(readOnly = true)
    public List<Payout> payouts() {
        LocalDate today = calendar.today();
        Map<String, BigDecimal> sums = new LinkedHashMap<>();
        for (SchoolLedgerEntry e : ledger.findByAtGreaterThanEqualOrderByAtDesc(today.minusDays(60).atStartOfDay())) {
            String src = switch (e.getSource()) {
                case COLLECTION, FEES -> "Collections and fees";
                case UNIFORM_HANDLING -> "Uniform shop handling fee";
                case SHOP_GIVEBACK -> "UniSchool give-back";
            };
            sums.merge(e.getAt().toLocalDate() + "|" + src, e.getAmount(), BigDecimal::add);
        }
        List<Payout> out = new ArrayList<>();
        sums.forEach((k, v) -> {
            LocalDate d = LocalDate.parse(k.substring(0, 10));
            out.add(new Payout(d, k.substring(11), Money.round(v), d.isBefore(today) ? "Settled" : "Pending, settles tomorrow"));
        });
        return out;
    }

    private AccessRequest pending(Long id) {
        AccessRequest r = requests.findById(id).orElseThrow(() -> new NotFoundException("Access request", id));
        if (r.getStatus() != AccessRequest.Status.PENDING) throw new ConflictException("This request has already been dealt with.");
        return r;
    }
}
