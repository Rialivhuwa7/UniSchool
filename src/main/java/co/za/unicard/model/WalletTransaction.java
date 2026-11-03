package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One movement on a wallet. Exactly one of {@code account} (a parent's wallet) or
 * {@code learner} (a child's card wallet) is set. Negative amounts are money out.
 */
@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(columnList = "account_id"), @Index(columnList = "learner_id")})
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private UserAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    private Learner learner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TxType type;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected WalletTransaction() {}

    private WalletTransaction(UserAccount account, Learner learner, TxType type, String description,
                              BigDecimal amount, LocalDateTime createdAt) {
        this.account = account;
        this.learner = learner;
        this.type = type;
        this.description = description;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public static WalletTransaction forAccount(UserAccount a, TxType type, String d, BigDecimal amount, LocalDateTime at) {
        return new WalletTransaction(a, null, type, d, amount, at);
    }

    public static WalletTransaction forLearner(Learner l, TxType type, String d, BigDecimal amount, LocalDateTime at) {
        return new WalletTransaction(null, l, type, d, amount, at);
    }

    public Long getId() { return id; }
    public UserAccount getAccount() { return account; }
    public Learner getLearner() { return learner; }
    public TxType getType() { return type; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
