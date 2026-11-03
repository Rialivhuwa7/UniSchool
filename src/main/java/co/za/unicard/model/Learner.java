package co.za.unicard.model;

import co.za.unicard.util.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * A child enrolled at one of the schools. Holds their card wallet and the controls their
 * parents set: daily limit, frozen card, tuckshop mode, blocked categories and approved items.
 */
@Entity
@Table(name = "learners")
public class Learner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String studentNumber;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    /** e.g. "Grade 4" */
    @Column(nullable = false)
    private String grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Campus campus;

    private String house;

    /** Token encoded in the card, wristband or phone QR that the till scans. */
    @Column(nullable = false, unique = true)
    private String cardToken = UUID.randomUUID().toString();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "learner_sports", joinColumns = @JoinColumn(name = "learner_id"))
    @Column(name = "sport")
    private Set<String> sports = new LinkedHashSet<>();

    /** Clothing sizes by type: Shirt, Shorts, Trousers, Shoes, Blazer, Jersey. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "learner_sizes", joinColumns = @JoinColumn(name = "learner_id"))
    @MapKeyColumn(name = "size_type")
    @Column(name = "size_value")
    private Map<String, String> sizes = new LinkedHashMap<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "learner_guardians",
            joinColumns = @JoinColumn(name = "learner_id"),
            inverseJoinColumns = @JoinColumn(name = "account_id"))
    private Set<UserAccount> guardians = new LinkedHashSet<>();

    // ---- wallet and controls ----
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal walletBalance = Money.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyLimit = Money.of(50);

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal spentToday = Money.ZERO;

    /** The day {@link #spentToday} belongs to; a new day resets the counter. */
    private LocalDate spentOn;

    private boolean frozen;

    /**
     * Optimistic locking: if two requests change this row at the same moment (e.g. a parent
     * moving money while the till charges the card), the second one fails instead of
     * overwriting the first.
     */
    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TuckMode tuckMode = TuckMode.FREE_CHOICE;

    /** Menu categories blocked in FREE_CHOICE mode, e.g. "Fizzy drinks". */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "learner_blocked_categories", joinColumns = @JoinColumn(name = "learner_id"))
    @Column(name = "category")
    private Set<String> blockedCategories = new LinkedHashSet<>();

    /** Menu item names allowed in APPROVED_ITEMS mode. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "learner_allowed_items", joinColumns = @JoinColumn(name = "learner_id"))
    @Column(name = "item_name")
    private Set<String> allowedItems = new LinkedHashSet<>();

    // ---- savings goal (money lessons in the learner's app) ----
    private String goalName;
    @Column(precision = 12, scale = 2)
    private BigDecimal goalTarget;
    @Column(precision = 12, scale = 2)
    private BigDecimal goalSaved = Money.ZERO;

    protected Learner() {}

    public Learner(String studentNumber, String firstName, String lastName, String grade, Campus campus, String house) {
        if (campus == Campus.BOTH) throw new IllegalArgumentException("A learner belongs to one campus");
        this.studentNumber = studentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.grade = grade;
        this.campus = campus;
        this.house = house;
    }

    /** Amount spent today; zero if the last spend was on an earlier day. */
    public BigDecimal spentOn(LocalDate today) {
        return today.equals(spentOn) ? spentToday : Money.ZERO;
    }

    public BigDecimal remainingToday(LocalDate today) {
        return dailyLimit.subtract(spentOn(today)).max(Money.ZERO);
    }

    public void recordSpend(BigDecimal amount, LocalDate today) {
        spentToday = spentOn(today).add(amount);
        spentOn = today;
    }

    public void credit(BigDecimal amount) { walletBalance = walletBalance.add(amount); }
    public void debit(BigDecimal amount) { walletBalance = walletBalance.subtract(amount); }

    public boolean isGuardian(Long accountId) {
        return guardians.stream().anyMatch(g -> g.getId().equals(accountId));
    }

    public void setGoal(String name, BigDecimal target, BigDecimal saved) {
        this.goalName = name;
        this.goalTarget = target;
        this.goalSaved = saved;
    }

    public void addToGoal(BigDecimal amount) { goalSaved = goalSaved.add(amount); }

    /** Corrections and moves by the school office, e.g. a new grade each year or moving to the College. */
    public void updateDetails(String firstName, String lastName, String grade, Campus campus, String house) {
        if (campus == Campus.BOTH) throw new IllegalArgumentException("A learner belongs to one campus");
        this.firstName = firstName;
        this.lastName = lastName;
        this.grade = grade;
        this.campus = campus;
        this.house = house;
    }

    public String getFullName() { return firstName + " " + lastName; }
    /** "Amara K." style, used where other families might see the name. */
    public String getShortName() { return firstName + " " + lastName.charAt(0) + "."; }

    public Long getId() { return id; }
    public String getStudentNumber() { return studentNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Campus getCampus() { return campus; }
    public String getHouse() { return house; }
    public String getCardToken() { return cardToken; }
    public Set<String> getSports() { return sports; }
    public Map<String, String> getSizes() { return sizes; }
    public Set<UserAccount> getGuardians() { return guardians; }
    public BigDecimal getWalletBalance() { return walletBalance; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
    public TuckMode getTuckMode() { return tuckMode; }
    public void setTuckMode(TuckMode tuckMode) { this.tuckMode = tuckMode; }
    public Set<String> getBlockedCategories() { return blockedCategories; }
    public Set<String> getAllowedItems() { return allowedItems; }
    public String getGoalName() { return goalName; }
    public BigDecimal getGoalTarget() { return goalTarget; }
    public BigDecimal getGoalSaved() { return goalSaved; }
}
