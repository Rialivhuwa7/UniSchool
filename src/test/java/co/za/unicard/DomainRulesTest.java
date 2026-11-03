package co.za.unicard;

import co.za.unicard.model.FeePlan;
import co.za.unicard.service.SchoolCalendar;
import co.za.unicard.util.Money;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit tests, no Spring context. */
class DomainRulesTest {

    @Test
    void feePlanAmounts() {
        var annual = Money.of(96000);
        assertThat(FeePlan.ANNUAL.instalment(annual)).isEqualByComparingTo("91200.00");     // 5% off
        assertThat(FeePlan.TERMLY.instalment(annual)).isEqualByComparingTo("24000.00");
        assertThat(FeePlan.MONTHLY_10.instalment(annual)).isEqualByComparingTo("9600.00");
        assertThat(FeePlan.FINANCED.instalment(annual)).isEqualByComparingTo("7800.00");    // 97.5% over 12
    }

    @Test
    void schoolDaysSkipWeekends() {
        // Friday 30 October 2026
        Clock friday = Clock.fixed(LocalDate.of(2026, 10, 30).atTime(9, 0).atZone(ZoneId.of("Africa/Johannesburg")).toInstant(),
                ZoneId.of("Africa/Johannesburg"));
        var days = new SchoolCalendar(friday).upcomingSchoolDays(3);
        assertThat(days).containsExactly(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3));
    }

    @Test
    void quietHoursDelayLateMessagesTo6am() {
        var cal = new SchoolCalendar(Clock.systemDefaultZone());
        var late = LocalDateTime.of(2026, 10, 26, 22, 15);
        assertThat(cal.deliveryTimeFor(late)).isEqualTo(LocalDateTime.of(2026, 10, 27, 6, 0));
        var early = LocalDateTime.of(2026, 10, 26, 5, 0);
        assertThat(cal.deliveryTimeFor(early)).isEqualTo(LocalDateTime.of(2026, 10, 26, 6, 0));
        var day = LocalDateTime.of(2026, 10, 26, 12, 0);
        assertThat(cal.deliveryTimeFor(day)).isEqualTo(day);
    }

    @Test
    void moneyFormatting() {
        assertThat(Money.format(Money.of("1234.5"))).isEqualTo("R1 234.50");
        assertThat(Money.pct(37, 62)).isEqualTo(60);
    }
}
