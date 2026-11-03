package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_contributions")
public class SavingsContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pot_id")
    private SavingsPot pot;

    /** "You", "Debit order", "Gogo Mavis"... */
    @Column(nullable = false)
    private String fromName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate contributedOn;

    protected SavingsContribution() {}

    SavingsContribution(SavingsPot pot, String fromName, BigDecimal amount, LocalDate contributedOn) {
        this.pot = pot;
        this.fromName = fromName;
        this.amount = amount;
        this.contributedOn = contributedOn;
    }

    public Long getId() { return id; }
    public String getFromName() { return fromName; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getContributedOn() { return contributedOn; }
}
