package co.za.unicard.model;


import co.za.unicard.util.Money;
import java.math.BigDecimal;

/** How a family pays next year's school fees. */
public enum FeePlan {
    ANNUAL("Once a year, upfront", "Pay in January and get 5% off.", 1),
    TERMLY("Per term", "Four payments, at the start of each term.", 4),
    MONTHLY_10("Monthly over 10 months", "Debit order January to October.", 10),
    MONTHLY_12("Monthly over 12 months", "Debit order January to December.", 12),
    FINANCED("Monthly, with upfront discount",
            "A finance partner pays the school upfront so you keep part of the 5% discount. Subject to a credit check by the partner, not UniSchool.", 12);

    private final String title;
    private final String description;
    private final int payments;

    FeePlan(String title, String description, int payments) {
        this.title = title;
        this.description = description;
        this.payments = payments;
    }

    /** The amount of each payment for a given annual fee. */
    public BigDecimal instalment(BigDecimal annual) {
        return switch (this) {
            case ANNUAL -> Money.round(annual.multiply(new BigDecimal("0.95")));
            case FINANCED -> Money.divide(annual.multiply(new BigDecimal("0.975")), 12);
            default -> Money.divide(annual, payments);
        };
    }

    public BigDecimal totalCost(BigDecimal annual) {
        return Money.round(instalment(annual).multiply(BigDecimal.valueOf(payments)));
    }

    public String title() { return title; }
    public String description() { return description; }
    public int payments() { return payments; }
}
