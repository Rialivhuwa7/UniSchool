package co.za.unicard.model;

/** The two Highveld Ridge schools. BOTH is used only for audiences (notices, collections). */
public enum Campus {
    PREP("Highveld Ridge Prep"),
    COLLEGE("Highveld Ridge College"),
    BOTH("Highveld Ridge Schools");

    private final String displayName;

    Campus(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    /** True if an audience aimed at this campus includes a learner at {@code learnerCampus}. */
    public boolean includes(Campus learnerCampus) { return this == BOTH || this == learnerCampus; }
}
