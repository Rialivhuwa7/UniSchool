package co.za.unicard.dto;

import co.za.unicard.model.Campus;
import co.za.unicard.model.CollectionPayment;
import co.za.unicard.model.FeePlan;
import co.za.unicard.model.SchoolCollection;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class PaymentDtos {
    private PaymentDtos() {}

    // ---- parent side ----
    public record CollectionForChild(Long collectionId, Long learnerId, String learnerName, String title,
                                     SchoolCollection.Kind kind, BigDecimal amount, LocalDate dueDate, int instalments,
                                     BigDecimal paid, BigDecimal owing, int ticketsBought, String status) {}

    public enum PayPart { FULL, INSTALMENT }

    /**
     * PAYMENT: part FULL (what's still owing) or INSTALMENT (amount / instalments).
     * TICKET: quantity. DONATION: amount.
     */
    public record PayRequest(@NotNull Long learnerId, PayPart part, @Min(1) @Max(10) Integer quantity,
                             @DecimalMin("10.00") BigDecimal amount, CollectionPayment.Method method) {}

    public record PaymentReceipt(String message, BigDecimal amountPaid, List<String> ticketCodes) {}

    public record TicketView(String code, String title, Long collectionId) {}

    public record PlanOption(FeePlan plan, String title, String description, int payments, BigDecimal eachPayment,
                             BigDecimal totalCost) {}

    public record FeeView(Long learnerId, String learnerName, String school, int year, BigDecimal annualFee,
                          FeePlan plan, String planTitle, BigDecimal eachPayment, int payments, LocalDate firstPayment,
                          boolean previousYearPaid, List<PlanOption> options) {}

    public record ChoosePlanRequest(@NotNull FeePlan plan) {}

    // ---- school office ----
    public record NewCollectionRequest(@NotBlank String title, @NotNull @DecimalMin("1.00") BigDecimal amount,
                                       @NotNull SchoolCollection.Kind kind, @NotNull Campus campus,
                                       @NotEmpty Set<String> grades, LocalDate dueDate, @Min(1) @Max(12) Integer instalments) {}

    public record CollectionStats(Long id, String title, SchoolCollection.Kind kind, BigDecimal amount, Campus campus,
                                  Set<String> grades, LocalDate dueDate, int instalments, long eligible, long paid,
                                  int percentPaid, BigDecimal collected) {}

    public record ReminderResult(long sentTo, String message) {}
}
