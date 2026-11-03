package co.za.unicard.model;

/** Lifecycle of teacher-authored notices and homework when the principal reviews drafts. */
public enum PublishStatus {
    PENDING,            // waiting for the principal
    CHANGES_REQUESTED,  // sent back to the teacher with a comment
    PUBLISHED           // visible to parents / learners
}
