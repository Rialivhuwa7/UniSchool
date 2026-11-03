package co.za.unicard.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** An event ticket with a code the gate scans (shown as a QR in the app). */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private SchoolCollection collection;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private UserAccount owner;

    private LocalDateTime issuedAt;
    private boolean scanned;

    protected Ticket() {}

    public Ticket(SchoolCollection collection, UserAccount owner, LocalDateTime issuedAt) {
        this.collection = collection;
        this.owner = owner;
        this.issuedAt = issuedAt;
    }

    public void assignCode() { this.code = "YE" + (4100 + id); }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public SchoolCollection getCollection() { return collection; }
    public UserAccount getOwner() { return owner; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public boolean isScanned() { return scanned; }
}
