package co.za.unicard.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public final class SavingsDtos {
    private SavingsDtos() {}

    public record ContributionView(String from, BigDecimal amount, LocalDate on) {}

    public record PotView(BigDecimal balance, BigDecimal goal, BigDecimal monthlyDebit, int debitsBeforeOpening,
                          BigDecimal projected, boolean onTrack, BigDecimal shortBy, LocalDate opensOn,
                          String shareLink, List<ContributionView> contributions) {}

    public record MonthlyRequest(@NotNull @DecimalMin("100.00") @DecimalMax("20000.00") BigDecimal monthlyDebit) {}

    public record PocketMoneyView(Long id, Long learnerId, String learnerName, BigDecimal amount, DayOfWeek dayOfWeek, boolean enabled) {}

    public record PocketMoneyRequest(@NotNull Long learnerId,
                                     @NotNull @DecimalMin("1.00") @DecimalMax("2000.00") BigDecimal amount,
                                     DayOfWeek dayOfWeek, Boolean enabled) {}

    public record PocketMoneyUpdate(@DecimalMin("1.00") @DecimalMax("2000.00") BigDecimal amount,
                                    DayOfWeek dayOfWeek, Boolean enabled) {}

    /** Public family contribution through the share link. */
    public record GiveRequest(@NotBlank @Size(max = 60) String fromName,
                              @NotNull @DecimalMin("10.00") @DecimalMax("20000.00") BigDecimal amount) {}

    public record GivePage(String forFamily, String message) {}
}
