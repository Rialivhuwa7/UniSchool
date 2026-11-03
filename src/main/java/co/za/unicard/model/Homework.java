package co.za.unicard.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "homework")
public class Homework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Campus campus;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String body;

    private LocalDate dueDate;

    private String attachmentName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_account_id")
    private UserAccount author;

    /** e.g. "Mrs Naidoo" */
    private String byLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublishStatus status;

    @Column(length = 1000)
    private String reviewComment;

    private LocalDateTime createdAt;

    protected Homework() {}

    public Homework(Campus campus, String grade, String subject, String title, String body, LocalDate dueDate,
                    String attachmentName, UserAccount author, String byLabel, PublishStatus status, LocalDateTime createdAt) {
        this.campus = campus;
        this.grade = grade;
        this.subject = subject;
        this.title = title;
        this.body = body;
        this.dueDate = dueDate;
        this.attachmentName = attachmentName;
        this.author = author;
        this.byLabel = byLabel;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void publish() {
        status = PublishStatus.PUBLISHED;
        reviewComment = null;
    }

    public void requestChanges(String comment) {
        status = PublishStatus.CHANGES_REQUESTED;
        reviewComment = comment;
    }

    public void edit(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public void resubmit() { status = PublishStatus.PENDING; }

    public Long getId() { return id; }
    public Campus getCampus() { return campus; }
    public String getGrade() { return grade; }
    public String getSubject() { return subject; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public LocalDate getDueDate() { return dueDate; }
    public String getAttachmentName() { return attachmentName; }
    public UserAccount getAuthor() { return author; }
    public String getByLabel() { return byLabel; }
    public PublishStatus getStatus() { return status; }
    public String getReviewComment() { return reviewComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
