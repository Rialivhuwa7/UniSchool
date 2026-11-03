package co.za.unicard.service;

import org.springframework.stereotype.Component;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * School days are Monday to Friday. Public holidays and term dates are not modelled yet.
 * Everything time-related goes through the injected Clock so tests can fix "now".
 */
@Component
public class SchoolCalendar {

    private final Clock clock;

    public SchoolCalendar(Clock clock) { this.clock = clock; }

    public LocalDate today() { return LocalDate.now(clock); }
    public LocalDateTime now() { return LocalDateTime.now(clock); }
    public LocalTime timeNow() { return LocalTime.now(clock); }

    public static boolean isSchoolDay(LocalDate d) {
        return d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    /** The next {@code count} school days, starting today if today is a school day. */
    public List<LocalDate> upcomingSchoolDays(int count) {
        List<LocalDate> out = new ArrayList<>();
        LocalDate d = today();
        while (out.size() < count) {
            if (isSchoolDay(d)) out.add(d);
            d = d.plusDays(1);
        }
        return out;
    }

    /** The day the tuckshop is serving: today, or the next school day on a weekend. */
    public LocalDate serviceDay() { return upcomingSchoolDays(1).get(0); }

    /** Parent group quiet hours: messages sent 21:00-06:00 are delivered at 06:00. */
    public LocalDateTime deliveryTimeFor(LocalDateTime sentAt) {
        LocalTime t = sentAt.toLocalTime();
        if (t.isBefore(LocalTime.of(6, 0))) return sentAt.toLocalDate().atTime(6, 0);
        if (!t.isBefore(LocalTime.of(21, 0))) return sentAt.toLocalDate().plusDays(1).atTime(6, 0);
        return sentAt;
    }
}
