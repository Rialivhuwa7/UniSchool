package co.za.unicard.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Rand amounts are BigDecimal with 2 decimal places, rounded half-up. */
public final class Money {
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Money() {}

    public static BigDecimal of(String value) { return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP); }
    public static BigDecimal of(long value) { return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP); }
    public static BigDecimal of(double value) { return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP); }
    public static BigDecimal round(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }

    /** value * percent / 100, e.g. percentOf(amount, 5) for a 5% give-back. */
    public static BigDecimal percentOf(BigDecimal value, int percent) {
        return round(value.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
    }

    public static BigDecimal divide(BigDecimal value, int parts) {
        return value.divide(BigDecimal.valueOf(parts), 2, RoundingMode.HALF_UP);
    }

    public static boolean isPositive(BigDecimal v) { return v != null && v.signum() > 0; }
    public static boolean lessThan(BigDecimal a, BigDecimal b) { return a.compareTo(b) < 0; }

    /** "R1 234.50": used in user-facing messages. */
    public static String format(BigDecimal v) {
        BigDecimal r = round(v);
        String sign = r.signum() < 0 ? "-" : "";
        return sign + "R" + String.format(java.util.Locale.ROOT, "%,.2f", r.abs()).replace(',', ' ');
    }

    public static int pct(long part, long whole) {
        if (whole <= 0) return 0;
        return (int) Math.max(0, Math.min(100, Math.round(part * 100.0 / whole)));
    }
}
