package co.za.unicard.dto;

import co.za.unicard.model.MenuItem;
import co.za.unicard.model.PreOrder;
import co.za.unicard.model.PreOrderLine;
import co.za.unicard.model.TuckMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class TuckshopDtos {
    private TuckshopDtos() {}

    public record MenuItemView(Long id, String name, BigDecimal price, String category) {
        public static MenuItemView of(MenuItem m) { return new MenuItemView(m.getId(), m.getName(), m.getPrice(), m.getCategory()); }
    }

    public record LineView(int index, String name, BigDecimal unitPrice, int quantity, boolean collected, LocalTime collectedAt) {}

    public record PreOrderView(Long id, Long learnerId, String learnerName, LocalDate day, List<LineView> lines,
                               BigDecimal total, PreOrder.PayFrom payFrom, PreOrder.Status status, boolean repeatWeekly) {
        public static PreOrderView of(PreOrder o) {
            List<LineView> lines = new java.util.ArrayList<>();
            for (int i = 0; i < o.getLines().size(); i++) {
                PreOrderLine l = o.getLines().get(i);
                lines.add(new LineView(i, l.getItemName(), l.getUnitPrice(), l.getQuantity(), l.isCollected(), l.getCollectedAt()));
            }
            return new PreOrderView(o.getId(), o.getLearner().getId(), o.getLearner().getFirstName(), o.getDay(), lines,
                    o.getTotal(), o.getPayFrom(), o.getStatus(), o.isRepeatWeekly());
        }
    }

    public record DayView(LocalDate day, boolean today, PreOrderView order) {}

    public record LearnerWeek(Long learnerId, String name, BigDecimal walletBalance, TuckMode mode, List<DayView> days) {}

    public record OrderItem(@NotNull Long menuItemId, @Min(1) @Max(5) int quantity) {}

    public record PlaceOrderRequest(
            @NotNull LocalDate day,
            @NotEmpty List<@Valid OrderItem> items,
            PreOrder.PayFrom payFrom,
            boolean repeatWeekly) {}

    // ---- till ----
    public record ChargeRequest(Long learnerId, String cardToken, @NotEmpty List<Long> menuItemIds) {}

    public record ChargeResult(boolean approved, String message, BigDecimal total, BigDecimal walletBalance) {}

    /** Line indexes to hand over; empty or null means everything not yet collected. */
    public record HandoutRequest(List<Integer> lineIndexes) {}

    public record HandoutResult(boolean ok, String message, PreOrderView order) {}

    public record TillLearnerView(Long id, String name, String grade, String school, BigDecimal walletBalance,
                                  BigDecimal leftToday, TuckMode mode, boolean frozen, PreOrderView todaysOrder) {}

    public record TillSummary(LocalDate serviceDay, int preOrders, long collectedInFull, BigDecimal salesToday,
                              long tillSalesToday, List<PreOrderView> orders) {}
}
