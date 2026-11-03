package co.za.unicard.model;

import jakarta.persistence.*;

import java.util.List;

/**
 * A moderated chat for the guardians of one grade or one sports team. Guardians are members
 * automatically through their children. In ANNOUNCE mode only the moderator and staff post.
 */
@Entity
@Table(name = "parent_groups")
public class ParentGroup {

    public enum Mode { OPEN, ANNOUNCE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Campus campus;

    /** Set for a grade group, e.g. "Grade 4". */
    private String grade;

    /** Set for a team group, e.g. "Hockey". */
    private String team;

    /** e.g. "Thandi K., class rep" */
    private String moderatorLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_account_id")
    private UserAccount moderator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mode mode = Mode.OPEN;

    protected ParentGroup() {}

    public ParentGroup(String name, Campus campus, String grade, String team, String moderatorLabel, UserAccount moderator, Mode mode) {
        this.name = name;
        this.campus = campus;
        this.grade = grade;
        this.team = team;
        this.moderatorLabel = moderatorLabel;
        this.moderator = moderator;
        this.mode = mode;
    }

    public boolean includes(Learner l) {
        if (!campus.includes(l.getCampus())) return false;
        if (grade != null) return grade.equals(l.getGrade());
        return team != null && l.getSports().contains(team);
    }

    public List<String> grades() { return grade == null ? List.of() : List.of(grade); }
    public List<String> teams() { return team == null ? List.of() : List.of(team); }

    public boolean isModerator(Long accountId) { return moderator != null && moderator.getId().equals(accountId); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Campus getCampus() { return campus; }
    public String getGrade() { return grade; }
    public String getTeam() { return team; }
    public String getModeratorLabel() { return moderatorLabel; }
    public UserAccount getModerator() { return moderator; }
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
}
