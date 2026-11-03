package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A learner's fees for one school year and the plan chosen to pay them. */
@Entity
@Table(name = "fee_accounts", uniqueConstraints = @UniqueConstraint(columnNames = {"learner_id", "fee_year"}))
public class FeeAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    private Learner learner;

    @Column(name = "fee_year", nullable = false)
    private int feeYear;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal annualFee;

    @Enumerated(EnumType.STRING)
    private FeePlan plan;

    /** Whether the previous year is settled. */
    private boolean previousYearPaid = true;

    private LocalDateTime planChosenAt;

    protected FeeAccount() {}

    public FeeAccount(Learner learner, int feeYear, BigDecimal annualFee, FeePlan plan) {
        this.learner = learner;
        this.feeYear = feeYear;
        this.annualFee = annualFee;
        this.plan = plan;
    }

    public void choosePlan(FeePlan plan, LocalDateTime at) {
        this.plan = plan;
        this.planChosenAt = at;
    }

    public Long getId() { return id; }
    public Learner getLearner() { return learner; }
    public int getFeeYear() { return feeYear; }
    public BigDecimal getAnnualFee() { return annualFee; }
    public void setAnnualFee(BigDecimal annualFee) { this.annualFee = annualFee; }
    public FeePlan getPlan() { return plan; }
    public boolean isPreviousYearPaid() { return previousYearPaid; }
}
