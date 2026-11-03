package co.za.unicard.dto;

import co.za.unicard.model.Campus;
import co.za.unicard.model.FeePlan;
import co.za.unicard.dto.PaymentDtos.CollectionStats;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class SchoolDtos {
    private SchoolDtos() {}

    public record FeedItem(LocalDateTime at, String description, BigDecimal amount, String payer) {}

    public record Overview(BigDecimal collected, long learnersWithFeePlan, long learnersTotal, int feePlanPercent,
                           BigDecimal giveBackThisYear, BigDecimal cashHandled, List<CollectionStats> openCollections,
                           List<FeedItem> latestPayments) {}

    /** `learners` describes the learners the request names, e.g. "Lerato Mokoena, Grade 4, Prep (in UniSchool)". */
    public record AccessRequestView(Long id, String name, String username, String ask, String how, String requestedRole,
                                    LocalDateTime requestedAt, List<String> learners) {}

    public record StaffView(String name, String role, String since) {}

    public record FeePlanBreakdown(int year, Map<String, Long> byPlan, long withoutPlan, long total) {}

    public record ListStatus(Campus campus, String grade, String status, long ordersPlaced, int items) {}

    public record ListItemRequest(@NotBlank String name, @Min(1) @Max(100) int quantity, @NotNull @DecimalMin("0.00") BigDecimal unitPrice) {}

    public record PublishListRequest(@NotEmpty List<@Valid ListItemRequest> items, boolean publish) {}

    public record StockLine(Long id, String item, String size, int quantity, boolean low, boolean restockRequested) {}

    public record UniformView(BigDecimal salesThisYear, long lowSizes, List<StockLine> stock) {}

    public record Payout(LocalDate date, String source, BigDecimal amount, String status) {}

    public record SettingsView(boolean approveNotices, boolean approveHomework) {}

    public record SettingsUpdate(Boolean approveNotices, Boolean approveHomework) {}

    public record Attention(String level, String message, String area) {}

    public record ApprovalItem(String type, Long id, String from, String title, String body, String audience, LocalDateTime submittedAt) {}

    public record PrincipalDashboard(int feePlanPercent, BigDecimal collected, int noticeReadPercent, int homeworkDonePercent,
                                     List<ApprovalItem> waitingForApproval, SettingsView settings, List<Attention> attention,
                                     long tuckshopPreOrdersToday, long tuckshopCollectedToday, BigDecimal tuckshopSalesToday,
                                     List<co.za.unicard.dto.HomeworkDtos.HomeworkStats> homework,
                                     List<co.za.unicard.dto.NoticeDtos.NoticeStats> notices) {}

    public record RosterRow(Long learnerId, String name, long homeworkDone, long homeworkSet, long acknowledged, long ackNeeded) {}

    public record ClassView(String teacher, String grade, String className, String school, List<RosterRow> learners) {}

    public record ReadinessItem(String label, boolean done, String area, Long learnerId) {}

    public record ParentHome(String greetingName, BigDecimal walletBalance, long unreadNotices, long readyCount,
                             List<ReadinessItem> readiness) {}

    public record Message(String message) {}

    public static String feePlanLabel(FeePlan p) { return p == null ? "None" : p.title(); }
}
