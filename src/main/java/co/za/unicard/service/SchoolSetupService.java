package co.za.unicard.service;

import co.za.unicard.dto.GroupDtos.GroupAdminView;
import co.za.unicard.dto.SchoolDtos.Message;
import co.za.unicard.dto.SchoolDtos.ListItemRequest;
import co.za.unicard.dto.SetupDtos.*;
import co.za.unicard.exception.ConflictException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.*;
import co.za.unicard.repository.*;
import co.za.unicard.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * The school office's setup work: the enrolment record, next year's fees, the uniform shop,
 * sport kit, parent groups and stationery lists. On a new school this is where all the data
 * the rest of the app relies on comes from.
 */
@Service
@Transactional
public class SchoolSetupService {

    private final LearnerRepository learners;
    private final UserAccountRepository accounts;
    private final FeeAccountRepository fees;
    private final UniformItemRepository uniformItems;
    private final UniformStockRepository stock;
    private final SportOfferRepository sport;
    private final ParentGroupRepository groups;
    private final SupplyListRepository lists;
    private final GroupService groupService;
    private final PaymentService payments;

    public SchoolSetupService(LearnerRepository learners, UserAccountRepository accounts, FeeAccountRepository fees,
                              UniformItemRepository uniformItems, UniformStockRepository stock, SportOfferRepository sport,
                              ParentGroupRepository groups, SupplyListRepository lists, GroupService groupService,
                              PaymentService payments) {
        this.learners = learners;
        this.accounts = accounts;
        this.fees = fees;
        this.uniformItems = uniformItems;
        this.stock = stock;
        this.sport = sport;
        this.groups = groups;
        this.lists = lists;
        this.groupService = groupService;
        this.payments = payments;
    }

    // ---- learners ----
    @Transactional(readOnly = true)
    public List<EnrolledLearner> learners(String q, Campus campus, String grade) {
        List<Learner> found = q != null && !q.isBlank() ? learners.search(q.trim()) : learners.findAll(org.springframework.data.domain.Sort.by("lastName", "firstName"));
        Set<Long> withSignIn = new HashSet<>(accounts.learnerIdsWithSignIn());
        return found.stream()
                .filter(l -> campus == null || campus == Campus.BOTH || l.getCampus() == campus)
                .filter(l -> grade == null || grade.isBlank() || l.getGrade().equals(grade))
                .map(l -> view(l, withSignIn.contains(l.getId())))
                .toList();
    }

    public EnrolledLearner enrol(EnrolLearnerRequest req) {
        String number = req.studentNumber().trim();
        if (learners.findByStudentNumber(number).isPresent()) {
            throw new ConflictException("Student number " + number + " is already on the enrolment record.");
        }
        Learner l = new Learner(number, req.firstName().trim(), req.lastName().trim(), req.grade().trim(), req.campus(), blankToNull(req.house()));
        if (req.sports() != null) l.getSports().addAll(clean(req.sports()));
        return view(learners.save(l), false);
    }

    public EnrolledLearner update(Long id, UpdateLearnerRequest req) {
        Learner l = learner(id);
        l.updateDetails(req.firstName().trim(), req.lastName().trim(), req.grade().trim(), req.campus(), blankToNull(req.house()));
        if (req.sports() != null) {
            l.getSports().clear();
            l.getSports().addAll(clean(req.sports()));
        }
        return view(l, accounts.learnerIdsWithSignIn().contains(id));
    }

    // ---- fees ----
    /** Next year's fees by grade: how many learners in each grade have a fee account loaded. */
    @Transactional(readOnly = true)
    public FeeSchedule feeSchedule() {
        int year = payments.feeYear();
        Map<Long, FeeAccount> byLearner = new HashMap<>();
        for (FeeAccount f : fees.findByFeeYear(year)) byLearner.put(f.getLearner().getId(), f);

        Map<String, List<Learner>> byGrade = new TreeMap<>();
        for (Learner l : learners.findAll()) byGrade.computeIfAbsent(l.getCampus() + "|" + l.getGrade(), k -> new ArrayList<>()).add(l);

        List<GradeFees> rows = new ArrayList<>();
        byGrade.forEach((key, kids) -> {
            List<FeeAccount> loaded = kids.stream().map(k -> byLearner.get(k.getId())).filter(Objects::nonNull).toList();
            BigDecimal fee = loaded.isEmpty() ? null : loaded.get(0).getAnnualFee();
            rows.add(new GradeFees(kids.get(0).getCampus(), kids.get(0).getGrade(), kids.size(), loaded.size(), fee));
        });
        rows.sort(Comparator.comparing(GradeFees::campus).thenComparing(g -> gradeOrder(g.grade())));
        return new FeeSchedule(year, rows);
    }

    /**
     * Sets next year's annual fee for every learner in a grade. Learners without a fee account
     * get one; those whose parents haven't chosen a plan yet get the new amount. Once a plan is
     * chosen the fee stays, because the debit order was approved for that amount.
     */
    public Message loadFees(LoadFeesRequest req) {
        if (req.campus() == Campus.BOTH) throw new IllegalArgumentException("Choose Prep or College.");
        int year = payments.feeYear();
        BigDecimal fee = Money.round(req.annualFee());
        List<Learner> kids = learners.findByCampusAndGrade(req.campus(), req.grade());
        if (kids.isEmpty()) throw new IllegalArgumentException("There are no learners in " + req.grade() + " at " + req.campus().displayName() + " yet.");
        int created = 0, updated = 0, kept = 0;
        for (Learner k : kids) {
            Optional<FeeAccount> existing = fees.findByLearnerIdAndFeeYear(k.getId(), year);
            if (existing.isEmpty()) {
                fees.save(new FeeAccount(k, year, fee, null));
                created++;
            } else if (existing.get().getPlan() == null) {
                existing.get().setAnnualFee(fee);
                updated++;
            } else {
                kept++;
            }
        }
        String msg = req.grade() + " " + year + " fees set to " + Money.format(fee) + " for " + (created + updated) + " learner"
                + (created + updated == 1 ? "" : "s") + ".";
        if (kept > 0) msg += " " + kept + " already chose a plan, so their fee stays as it was.";
        return new Message(msg);
    }

    // ---- uniform shop ----
    @Transactional(readOnly = true)
    public List<UniformItemView> uniformItems() {
        return uniformItems.findAll(org.springframework.data.domain.Sort.by("campus", "name")).stream().map(this::view).toList();
    }

    public UniformItemView addUniformItem(UniformItemRequest req) {
        if (req.campus() == Campus.BOTH) throw new IllegalArgumentException("Choose Prep or College.");
        return view(uniformItems.save(new UniformItem(req.campus(), req.name().trim(), Money.round(req.price()), req.kind(),
                req.sizeType().trim(), blankToNull(req.supplier()))));
    }

    public UniformItemView priceUniformItem(Long id, PriceRequest req) {
        UniformItem i = uniformItems.findById(id).orElseThrow(() -> new NotFoundException("Uniform item", id));
        i.setPrice(Money.round(req.price()));
        return view(i);
    }

    public void addStock(StockRequest req) {
        String item = req.itemName().trim(), size = req.size().trim();
        boolean exists = stock.findAll().stream().anyMatch(s -> s.getItemName().equalsIgnoreCase(item) && s.getSize().equalsIgnoreCase(size));
        if (exists) throw new ConflictException(item + " size " + size + " is already in stock. Change its quantity instead.");
        stock.save(new UniformStock(item, size, req.quantity()));
    }

    public void setStock(Long id, QuantityRequest req) {
        UniformStock s = stock.findById(id).orElseThrow(() -> new NotFoundException("Stock line", id));
        s.setQuantity(req.quantity());
        if (!s.isLow()) s.setRestockRequested(false);
    }

    // ---- sport kit ----
    @Transactional(readOnly = true)
    public List<SportOfferView> sportOffers() {
        return sport.findAll(org.springframework.data.domain.Sort.by("sport", "name")).stream().map(this::view).toList();
    }

    public SportOfferView addSportOffer(SportOfferRequest req) {
        return view(sport.save(new SportOffer(req.sport().trim(), req.type(), req.name().trim(), Money.round(req.price()))));
    }

    public SportOfferView priceSportOffer(Long id, PriceRequest req) {
        SportOffer o = sport.findById(id).orElseThrow(() -> new NotFoundException("Sport item", id));
        o.setPrice(Money.round(req.price()));
        return view(o);
    }

    // ---- parent groups ----
    public GroupAdminView createGroup(NewGroupRequest req) {
        String grade = blankToNull(req.grade()), team = blankToNull(req.team());
        if (grade == null && team == null) throw new IllegalArgumentException("Choose a grade or a team for the group.");
        String moderator = blankToNull(req.moderatorLabel());
        ParentGroup g = groups.save(new ParentGroup(req.name().trim(), req.campus(), grade, team,
                moderator == null ? "School office" : moderator, null, req.mode()));
        return groupService.allGroups().stream().filter(v -> v.id().equals(g.getId())).findFirst().orElseThrow();
    }

    // ---- stationery lists ----
    @Transactional(readOnly = true)
    public SupplyListDetail supplyList(Campus campus, String grade) {
        return lists.findByCampusAndGrade(campus, grade)
                .map(l -> new SupplyListDetail(campus, grade, l.getStatus() == SupplyList.Status.PUBLISHED ? "Published" : "Draft",
                        l.getItems().stream().map(i -> new ListItemRequest(i.getName(), i.getQuantity(), i.getUnitPrice())).toList()))
                .orElse(new SupplyListDetail(campus, grade, "Not started", List.of()));
    }

    // ---- helpers ----
    private Learner learner(Long id) {
        return learners.findById(id).orElseThrow(() -> new NotFoundException("Learner", id));
    }

    private EnrolledLearner view(Learner l, boolean hasSignIn) {
        return new EnrolledLearner(l.getId(), l.getStudentNumber(), l.getFirstName(), l.getLastName(), l.getGrade(), l.getCampus(),
                l.getHouse(), Set.copyOf(l.getSports()), l.getGuardians().size(), hasSignIn);
    }

    private UniformItemView view(UniformItem i) {
        return new UniformItemView(i.getId(), i.getCampus(), i.getName(), i.getPrice(), i.getKind(), i.getSizeType(), i.getSupplier());
    }

    private SportOfferView view(SportOffer o) {
        return new SportOfferView(o.getId(), o.getSport(), o.getType(), o.getName(), o.getPrice());
    }

    private static Set<String> clean(Set<String> values) {
        Set<String> out = new LinkedHashSet<>();
        for (String v : values) if (v != null && !v.isBlank()) out.add(v.trim());
        return out;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** "Grade R" before "Grade 1" before "Grade 10". */
    private static int gradeOrder(String grade) {
        String n = grade.replace("Grade", "").trim();
        if (n.equalsIgnoreCase("R")) return 0;
        try {
            return Integer.parseInt(n);
        } catch (NumberFormatException e) {
            return 99;
        }
    }
}
