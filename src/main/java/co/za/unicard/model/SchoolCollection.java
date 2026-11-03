package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Something the school collects money for: a trip or levy (PAYMENT, possibly in instalments),
 * event tickets (TICKET, price per ticket) or a fund (DONATION, any amount).
 */
@Entity
@Table(name = "collections")
public class SchoolCollection {

    public enum Kind { PAYMENT, TICKET, DONATION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /** Full amount (PAYMENT), price per ticket (TICKET) or suggested amount (DONATION). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Campus campus;

    /** Grades it applies to; "All" for the whole school. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "collection_grades", joinColumns = @JoinColumn(name = "collection_id"))
    @Column(name = "grade")
    private Set<String> grades = new LinkedHashSet<>();

    /** Null means open-ended. */
    private LocalDate dueDate;

    /** Number of instalments allowed for a PAYMENT. */
    private int instalments = 1;

    private LocalDateTime createdAt;

    protected SchoolCollection() {}

    public SchoolCollection(String title, BigDecimal amount, Kind kind, Campus campus, Set<String> grades,
                            LocalDate dueDate, int instalments, LocalDateTime createdAt) {
        this.title = title;
        this.amount = amount;
        this.kind = kind;
        this.campus = campus;
        this.grades.addAll(grades);
        this.dueDate = dueDate;
        this.instalments = kind == Kind.PAYMENT ? Math.max(1, instalments) : 1;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public Kind getKind() { return kind; }
    public Campus getCampus() { return campus; }
    public Set<String> getGrades() { return grades; }
    public LocalDate getDueDate() { return dueDate; }
    public int getInstalments() { return instalments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
