package co.za.unicard.model;

import jakarta.persistence.*;

/** School-wide switches the principal controls. Single row with id 1. */
@Entity
@Table(name = "school_settings")
public class SchoolSettings {

    public static final long ID = 1L;

    @Id
    private Long id = ID;

    /** Teachers' notices go to the principal before parents see them. */
    private boolean approveNotices = true;

    /** Homework goes to the principal before learners see it. */
    private boolean approveHomework = false;

    public SchoolSettings() {}

    public Long getId() { return id; }
    public boolean isApproveNotices() { return approveNotices; }
    public void setApproveNotices(boolean approveNotices) { this.approveNotices = approveNotices; }
    public boolean isApproveHomework() { return approveHomework; }
    public void setApproveHomework(boolean approveHomework) { this.approveHomework = approveHomework; }
}
