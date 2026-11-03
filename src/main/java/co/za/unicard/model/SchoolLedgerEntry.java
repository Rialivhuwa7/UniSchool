package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Money owed to the school. Entries settle into the school's bank account the next day
 * (a payout). The "latest payments" feed and the payouts list both read from here.
 */
@Entity
@Table(name = "school_ledger")
public class SchoolLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime at;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Who paid, shown as "A. Dlamini". Null for give-backs. */
    private String payerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerSource source;

    protected SchoolLedgerEntry() {}

    public SchoolLedgerEntry(LocalDateTime at, String description, BigDecimal amount, String payerName, LedgerSource source) {
        this.at = at;
        this.description = description;
        this.amount = amount;
        this.payerName = payerName;
        this.source = source;
    }

    /** Settles the day after it was received. */
    public boolean isSettled(LocalDate today) { return at.toLocalDate().isBefore(today); }

    public Long getId() { return id; }
    public LocalDateTime getAt() { return at; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getPayerName() { return payerName; }
    public LedgerSource getSource() { return source; }
}
