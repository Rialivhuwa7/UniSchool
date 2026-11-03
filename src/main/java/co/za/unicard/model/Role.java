package co.za.unicard.model;

/** What a person can do in UniSchool. One account can hold several roles and switch between them. */
public enum Role {
    PARENT,
    LEARNER,
    TEACHER,
    PRINCIPAL,
    SCHOOL_OFFICE,
    TUCKSHOP;

    public boolean isStaff() { return this == TEACHER || this == PRINCIPAL || this == SCHOOL_OFFICE || this == TUCKSHOP; }
}
