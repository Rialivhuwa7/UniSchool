package co.za.unicard.model;

import jakarta.persistence.*;

/**
 * One role held by an account. Staff roles are scoped to a campus (and for teachers a grade
 * and class). A LEARNER role points at the learner record. Roles only work once verified.
 */
@Entity
@Table(name = "role_assignments")
public class RoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private UserAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** e.g. "Grade 4 teacher, Highveld Ridge Prep" */
    private String label;

    @Enumerated(EnumType.STRING)
    private Campus campus;

    private String grade;      // teachers
    private String className;  // teachers, e.g. "4B"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    private Learner learner;   // LEARNER role only

    private boolean verified;
    private String verifiedNote; // e.g. "Verified 2 Feb 2026"
    private int position;

    protected RoleAssignment() {}

    public RoleAssignment(Role role, String label, Campus campus, boolean verified) {
        this.role = role;
        this.label = label;
        this.campus = campus;
        this.verified = verified;
    }

    public static RoleAssignment teacher(String label, Campus campus, String grade, String className) {
        RoleAssignment r = new RoleAssignment(Role.TEACHER, label, campus, true);
        r.grade = grade;
        r.className = className;
        return r;
    }

    public static RoleAssignment learner(Learner learner) {
        RoleAssignment r = new RoleAssignment(Role.LEARNER,
                learner.getGrade() + ", " + learner.getCampus().displayName(), learner.getCampus(), true);
        r.learner = learner;
        return r;
    }

    void attach(UserAccount account, int position) {
        this.account = account;
        this.position = position;
    }

    public void verify(String note) {
        this.verified = true;
        this.verifiedNote = note;
    }

    /** Links a learner's own sign-in to their learner record, once the office has approved it. */
    public void linkLearner(Learner learner) {
        this.learner = learner;
        this.label = learner.getGrade() + ", " + learner.getCampus().displayName();
        this.campus = learner.getCampus();
    }

    /** The school office says what a newly approved teacher teaches. */
    public void assignTeaching(String label, Campus campus, String grade, String className) {
        this.label = label;
        this.campus = campus;
        this.grade = grade;
        this.className = className;
    }

    public Long getId() { return id; }
    public UserAccount getAccount() { return account; }
    public Role getRole() { return role; }
    public String getLabel() { return label; }
    public Campus getCampus() { return campus; }
    public String getGrade() { return grade; }
    public String getClassName() { return className; }
    public Learner getLearner() { return learner; }
    public boolean isVerified() { return verified; }
    public String getVerifiedNote() { return verifiedNote; }
    public void setVerifiedNote(String verifiedNote) { this.verifiedNote = verifiedNote; }
}
