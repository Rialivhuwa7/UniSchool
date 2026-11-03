package co.za.unicard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.time.LocalTime;

@Embeddable
public class PreOrderLine {

    @Column(nullable = false)
    private String itemName;

    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    private int quantity;

    private boolean collected;
    private LocalTime collectedAt;

    protected PreOrderLine() {}

    public PreOrderLine(String itemName, String category, BigDecimal unitPrice, int quantity) {
        this.itemName = itemName;
        this.category = category;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public BigDecimal lineTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }

    public void markCollected(LocalTime at) {
        this.collected = true;
        this.collectedAt = at;
    }

    public PreOrderLine copy() { return new PreOrderLine(itemName, category, unitPrice, quantity); }

    public String label() { return (quantity > 1 ? quantity + " x " : "") + itemName; }

    public String getItemName() { return itemName; }
    public String getCategory() { return category; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public boolean isCollected() { return collected; }
    public LocalTime getCollectedAt() { return collectedAt; }
}
