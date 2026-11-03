package co.za.unicard.model;

import co.za.unicard.util.Money;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A parent's back-to-school savings pot. A monthly debit order runs on {@link #DEBIT_DAY};
 * family can add to it through a share link. It opens on {@code opensOn} to pay January costs.
 */
@Entity
@Table(name = "savings_pots")
public class SavingsPot {

    public static final int DEBIT_DAY = 25;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", unique = true)
    private UserAccount account;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = Money.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal goal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyDebit;

    @Column(nullable = false)
    private LocalDate opensOn;

    @Version
    private Long version;

    /** Used in the family share link: /api/give/{shareToken}. */
    @Column(nullable = false, unique = true)
    private String shareToken = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    @OneToMany(mappedBy = "pot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("contributedOn DESC, id DESC")
    private List<SavingsContribution> contributions = new ArrayList<>();

    protected SavingsPot() {}

    public SavingsPot(UserAccount account, BigDecimal balance, BigDecimal goal, BigDecimal monthlyDebit, LocalDate opensOn) {
        this.account = account;
        this.balance = balance;
        this.goal = goal;
        this.monthlyDebit = monthlyDebit;
        this.opensOn = opensOn;
    }

    public SavingsContribution contribute(String from, BigDecimal amount, LocalDate on) {
        SavingsContribution c = new SavingsContribution(this, from, amount, on);
        contributions.add(c);
        balance = balance.add(amount);
        return c;
    }

    public void withdraw(BigDecimal amount) { balance = balance.subtract(amount); }

    /** How many debit orders still run before the pot opens. */
    public int debitsRemaining(LocalDate today) {
        int n = 0;
        LocalDate d = today.withDayOfMonth(DEBIT_DAY);
        if (!d.isAfter(today)) d = d.plusMonths(1);
        while (d.isBefore(opensOn)) {
            n++;
            d = d.plusMonths(1);
        }
        return n;
    }

    public BigDecimal projected(LocalDate today) {
        return balance.add(monthlyDebit.multiply(BigDecimal.valueOf(debitsRemaining(today))));
    }

    public Long getId() { return id; }
    public UserAccount getAccount() { return account; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getGoal() { return goal; }
    public void setGoal(BigDecimal goal) { this.goal = goal; }
    public BigDecimal getMonthlyDebit() { return monthlyDebit; }
    public void setMonthlyDebit(BigDecimal monthlyDebit) { this.monthlyDebit = monthlyDebit; }
    public LocalDate getOpensOn() { return opensOn; }
    public String getShareToken() { return shareToken; }
    public List<SavingsContribution> getContributions() { return contributions; }
}
