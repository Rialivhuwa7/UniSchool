package co.za.unicard.util;


import co.za.unicard.model.Campus;
import co.za.unicard.model.Learner;
import java.util.Collection;

/**
 * Audience rules shared by notices, collections and groups. A learner is in the audience if
 * their campus matches AND either their grade is listed ("All" means every grade) or they
 * play one of the listed sports teams.
 */
public final class Audience {
    public static final String ALL_GRADES = "All";

    private Audience() {}

    public static boolean matches(Learner l, Campus campus, Collection<String> grades, Collection<String> teams) {
        if (campus != null && !campus.includes(l.getCampus())) return false;
        boolean gradeOk = grades != null && (grades.contains(ALL_GRADES) || grades.contains(l.getGrade()));
        boolean teamOk = teams != null && teams.stream().anyMatch(t -> l.getSports().contains(t));
        return gradeOk || teamOk;
    }
}
