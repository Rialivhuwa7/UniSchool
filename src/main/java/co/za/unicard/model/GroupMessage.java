package co.za.unicard.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_messages", indexes = @Index(columnList = "group_id,deliver_at"))
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private ParentGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_account_id")
    private UserAccount author;

    @Column(nullable = false)
    private String authorName;

    /** "Class rep", "Coach", "Teacher" or null for parents. */
    private String authorRole;

    @Column(nullable = false, length = 2000)
    private String body;

    private LocalDateTime sentAt;

    /** Quiet hours: messages sent 21:00-06:00 are delivered to others at 06:00. */
    @Column(name = "deliver_at")
    private LocalDateTime deliverAt;

    private int reports;
    private boolean hidden;

    protected GroupMessage() {}

    public GroupMessage(ParentGroup group, UserAccount author, String authorName, String authorRole, String body,
                        LocalDateTime sentAt, LocalDateTime deliverAt) {
        this.group = group;
        this.author = author;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.body = body;
        this.sentAt = sentAt;
        this.deliverAt = deliverAt;
    }

    public boolean isBy(Long accountId) { return author != null && author.getId().equals(accountId); }
    public void report() { reports++; }
    public void hide() { hidden = true; }

    public Long getId() { return id; }
    public ParentGroup getGroup() { return group; }
    public UserAccount getAuthor() { return author; }
    public String getAuthorName() { return authorName; }
    public String getAuthorRole() { return authorRole; }
    public String getBody() { return body; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getDeliverAt() { return deliverAt; }
    public int getReports() { return reports; }
    public boolean isHidden() { return hidden; }
}
