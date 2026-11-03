package co.za.unicard.dto;

import co.za.unicard.model.Campus;
import co.za.unicard.model.Learner;
import co.za.unicard.model.TuckMode;
import co.za.unicard.model.WalletTransaction;
import co.za.unicard.util.Money;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public final class LearnerDtos {
    private LearnerDtos() {}

    public record Goal(String name, BigDecimal target, BigDecimal saved, int percent) {}

    public record LearnerView(Long id, String studentNumber, String firstName, String lastName, String grade,
                              Campus campus, String school, String house, Set<String> sports, Map<String, String> sizes,
                              BigDecimal walletBalance, BigDecimal dailyLimit, BigDecimal spentToday,
                              boolean frozen, TuckMode tuckMode, Set<String> blockedCategories, Set<String> allowedItems,
                              Goal goal) {
        public static LearnerView of(Learner l, LocalDate today) {
            Goal goal = l.getGoalName() == null ? null : new Goal(l.getGoalName(), l.getGoalTarget(), l.getGoalSaved(),
                    l.getGoalTarget() == null ? 0 : Money.pct(l.getGoalSaved().longValue(), l.getGoalTarget().longValue()));
            return new LearnerView(l.getId(), l.getStudentNumber(), l.getFirstName(), l.getLastName(), l.getGrade(),
                    l.getCampus(), l.getCampus().displayName(), l.getHouse(), Set.copyOf(l.getSports()), Map.copyOf(l.getSizes()),
                    l.getWalletBalance(), l.getDailyLimit(), l.spentOn(today), l.isFrozen(), l.getTuckMode(),
                    Set.copyOf(l.getBlockedCategories()), Set.copyOf(l.getAllowedItems()), goal);
        }
    }

    /** Partial update of card controls; null fields are left unchanged. */
    public record ControlsRequest(
            @DecimalMin("0.00") @DecimalMax("1000.00") BigDecimal dailyLimit,
            Boolean frozen,
            TuckMode tuckMode,
            Set<String> blockedCategories,
            Set<String> allowedItems) {}

    public record AmountRequest(@NotNull @DecimalMin("1.00") @DecimalMax("50000.00") BigDecimal amount) {}

    public record TxView(Long id, String who, Long learnerId, String description, BigDecimal amount, String type, LocalDateTime at) {
        public static TxView of(WalletTransaction t) {
            Learner l = t.getLearner();
            return new TxView(t.getId(), l == null ? "You" : l.getFirstName(), l == null ? null : l.getId(),
                    t.getDescription(), t.getAmount(), t.getType().name(), t.getCreatedAt());
        }
    }

    /** What the learner's own app shows: their card (QR token), wallet and goal. */
    public record CardView(LearnerView learner, String cardToken) {}
}
