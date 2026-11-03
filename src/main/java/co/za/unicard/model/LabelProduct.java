package co.za.unicard.model;


import co.za.unicard.util.Money;
import java.math.BigDecimal;

/** Name labels printed with the child's name. Fixed products, not stored in the database. */
public enum LabelProduct {
    IRON_ON("Iron-on clothing labels, 100", 165, "Printed with the child's name, delivered with the uniform."),
    STICK_ON("Stick-on labels for books and bottles, 60", 120, "Waterproof, dishwasher safe."),
    QR_TAGS("Lost-property QR tags, 20", 95, "Whoever finds an item scans the tag and you get a message. Your number stays private.");

    private final String title;
    private final BigDecimal price;
    private final String description;

    LabelProduct(String title, long price, String description) {
        this.title = title;
        this.price = Money.of(price);
        this.description = description;
    }

    public String title() { return title; }
    public BigDecimal price() { return price; }
    public String description() { return description; }
}
