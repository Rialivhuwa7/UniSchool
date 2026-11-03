package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/** A weekly transfer from a parent's wallet to a child's card wallet. */
@Entity
@Table(name = "pocket_money_rules")
public class PocketMoneyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private UserAccount account;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    private Learner learner;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek = DayOfWeek.MONDAY;

    private boolean enabled = true;

    protected PocketMoneyRule() {}

    public PocketMoneyRule(UserAccount account, Learner learner, BigDecimal amount, DayOfWeek dayOfWeek, boolean enabled) {
        this.account = account;
        this.learner = learner;
        this.amount = amount;
        this.dayOfWeek = dayOfWeek;
        this.enabled = enabled;
    }

    public Long getId() { return id; }
    public UserAccount getAccount() { return account; }
    public Learner getLearner() { return learner; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
