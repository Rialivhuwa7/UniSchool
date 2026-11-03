package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Food ordered for one learner on one school day. Paid upfront from the parent's wallet or
 * the learner's own wallet. At break the learner taps their card and the tuckshop hands it over,
 * possibly in parts.
 */
@Entity
@Table(name = "pre_orders", uniqueConstraints = @UniqueConstraint(columnNames = {"learner_id", "service_day"}))
public class PreOrder {

    public enum Status { WAITING, PARTIAL, COLLECTED }
    public enum PayFrom { PARENT, LEARNER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    private Learner learner;

    /** "day" is a reserved word in H2, hence the column name. */
    @Column(name = "service_day", nullable = false)
    private LocalDate day;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pre_order_lines", joinColumns = @JoinColumn(name = "pre_order_id"))
    @OrderColumn(name = "line_no")
    private List<PreOrderLine> lines = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayFrom payFrom;

    /** The parent who paid, when payFrom is PARENT. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_account_id")
    private UserAccount payer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.WAITING;

    private boolean repeatWeekly;
    private LocalTime lastCollectedAt;
    private LocalDateTime createdAt;

    protected PreOrder() {}

    public PreOrder(Learner learner, LocalDate day, List<PreOrderLine> lines, PayFrom payFrom, UserAccount payer,
                    boolean repeatWeekly, LocalDateTime createdAt) {
        this.learner = learner;
        this.day = day;
        this.lines.addAll(lines);
        this.total = lines.stream().map(PreOrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.payFrom = payFrom;
        this.payer = payer;
        this.repeatWeekly = repeatWeekly;
        this.createdAt = createdAt;
    }

    /** Re-computes the status after lines were handed over. */
    public void refreshStatus(LocalTime at) {
        long done = lines.stream().filter(PreOrderLine::isCollected).count();
        status = done == 0 ? Status.WAITING : done == lines.size() ? Status.COLLECTED : Status.PARTIAL;
        lastCollectedAt = at;
    }

    public String itemsText() {
        return String.join(", ", lines.stream().map(PreOrderLine::label).toList());
    }

    public Long getId() { return id; }
    public Learner getLearner() { return learner; }
    public LocalDate getDay() { return day; }
    public List<PreOrderLine> getLines() { return lines; }
    public BigDecimal getTotal() { return total; }
    public PayFrom getPayFrom() { return payFrom; }
    public UserAccount getPayer() { return payer; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public boolean isRepeatWeekly() { return repeatWeekly; }
    public LocalTime getLastCollectedAt() { return lastCollectedAt; }
}
