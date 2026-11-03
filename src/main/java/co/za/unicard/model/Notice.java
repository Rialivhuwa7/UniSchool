package co.za.unicard.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/** A notice-board post to guardians of a grade, a sports team or the whole school. */
@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_account_id")
    private UserAccount author;

    /** e.g. "Mrs Naidoo, Grade 4 teacher", "Principal's office", "Sports office" */
    @Column(nullable = false)
    private String fromLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Campus campus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notice_grades", joinColumns = @JoinColumn(name = "notice_id"))
    @Column(name = "grade")
    private Set<String> grades = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notice_teams", joinColumns = @JoinColumn(name = "notice_id"))
    @Column(name = "team")
    private Set<String> teams = new LinkedHashSet<>();

    /** Action needed, Event, Change, Reminder */
    private String tag;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String body;

    private String attachmentName;

    /** Guardians must acknowledge, once per child. */
    private boolean requiresAck;

    /** Guardians can say whether they're coming. */
    private boolean rsvpEnabled;

    /** A payment this notice points to, e.g. camp fees. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private SchoolCollection linkedCollection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublishStatus status;

    /** Principal's comment when asking for changes. */
    @Column(length = 1000)
    private String reviewComment;

    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    protected Notice() {}

    public Notice(UserAccount author, String fromLabel, Campus campus, Set<String> grades, Set<String> teams, String tag,
                  String title, String body, String attachmentName, boolean requiresAck, boolean rsvpEnabled,
                  SchoolCollection linkedCollection, PublishStatus status, LocalDateTime createdAt) {
        this.author = author;
        this.fromLabel = fromLabel;
        this.campus = campus;
        if (grades != null) this.grades.addAll(grades);
        if (teams != null) this.teams.addAll(teams);
        this.tag = tag;
        this.title = title;
        this.body = body;
        this.attachmentName = attachmentName;
        this.requiresAck = requiresAck;
        this.rsvpEnabled = rsvpEnabled;
        this.linkedCollection = linkedCollection;
        this.status = status;
        this.createdAt = createdAt;
        if (status == PublishStatus.PUBLISHED) this.publishedAt = createdAt;
    }

    public void publish(LocalDateTime at) {
        status = PublishStatus.PUBLISHED;
        publishedAt = at;
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

    /** "Grade 4, Prep" / "Hockey team, Prep" / "Whole school" */
    public String audienceText() {
        StringBuilder sb = new StringBuilder();
        if (!grades.isEmpty()) sb.append(grades.contains("All") ? "Whole school" : String.join(", ", grades));
        if (!teams.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(String.join(", ", teams)).append(" team");
        }
        if (campus != Campus.BOTH) sb.append(", ").append(campus.displayName().replace("Highveld Ridge ", ""));
        return sb.toString();
    }

    public Long getId() { return id; }
    public UserAccount getAuthor() { return author; }
    public String getFromLabel() { return fromLabel; }
    public Campus getCampus() { return campus; }
    public Set<String> getGrades() { return grades; }
    public Set<String> getTeams() { return teams; }
    public String getTag() { return tag; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getAttachmentName() { return attachmentName; }
    public boolean isRequiresAck() { return requiresAck; }
    public boolean isRsvpEnabled() { return rsvpEnabled; }
    public SchoolCollection getLinkedCollection() { return linkedCollection; }
    public PublishStatus getStatus() { return status; }
    public String getReviewComment() { return reviewComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
}
