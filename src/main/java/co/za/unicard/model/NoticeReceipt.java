package co.za.unicard.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A guardian's interaction with a notice. READ is per guardian; ACK is per guardian per child;
 * RSVP is per guardian with a YES/NO answer.
 */
@Entity
@Table(name = "notice_receipts", indexes = @Index(columnList = "notice_id"))
public class NoticeReceipt {

    public enum Type { READ, ACK, RSVP }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private Notice notice;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private UserAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    private Learner learner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    /** RSVP answer: true = coming. */
    private Boolean attending;

    @Column(name = "occurred_at")
    private LocalDateTime at;

    protected NoticeReceipt() {}

    public NoticeReceipt(Notice notice, UserAccount account, Learner learner, Type type, Boolean attending, LocalDateTime at) {
        this.notice = notice;
        this.account = account;
        this.learner = learner;
        this.type = type;
        this.attending = attending;
        this.at = at;
    }

    public Long getId() { return id; }
    public Notice getNotice() { return notice; }
    public UserAccount getAccount() { return account; }
    public Learner getLearner() { return learner; }
    public Type getType() { return type; }
    public Boolean getAttending() { return attending; }
    public void setAttending(Boolean attending) { this.attending = attending; }
}
