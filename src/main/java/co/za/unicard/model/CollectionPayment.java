package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "collection_payments", indexes = @Index(columnList = "collection_id,learner_id"))
public class CollectionPayment {

    public enum Method { WALLET, CARD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private SchoolCollection collection;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    private Learner learner;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_account_id")
    private UserAccount payer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private int tickets;

    @Enumerated(EnumType.STRING)
    private Method method;

    private LocalDateTime paidAt;

    protected CollectionPayment() {}

    public CollectionPayment(SchoolCollection collection, Learner learner, UserAccount payer, BigDecimal amount,
                             int tickets, Method method, LocalDateTime paidAt) {
        this.collection = collection;
        this.learner = learner;
        this.payer = payer;
        this.amount = amount;
        this.tickets = tickets;
        this.method = method;
        this.paidAt = paidAt;
    }

    public Long getId() { return id; }
    public SchoolCollection getCollection() { return collection; }
    public Learner getLearner() { return learner; }
    public UserAccount getPayer() { return payer; }
    public BigDecimal getAmount() { return amount; }
    public int getTickets() { return tickets; }
    public Method getMethod() { return method; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
