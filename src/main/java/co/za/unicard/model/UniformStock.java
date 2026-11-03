package co.za.unicard.model;

import jakarta.persistence.*;

/** Stock of crested items held by the school shop, per size. */
@Entity
@Table(name = "uniform_stock", uniqueConstraints = @UniqueConstraint(columnNames = {"item_name", "stock_size"}))
public class UniformStock {

    public static final int LOW = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "stock_size", nullable = false)
    private String size;

    private int quantity;

    private boolean restockRequested;

    protected UniformStock() {}

    public UniformStock(String itemName, String size, int quantity) {
        this.itemName = itemName;
        this.size = size;
        this.quantity = quantity;
    }

    public boolean isLow() { return quantity <= LOW; }

    public Long getId() { return id; }
    public String getItemName() { return itemName; }
    public String getSize() { return size; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public boolean isRestockRequested() { return restockRequested; }
    public void setRestockRequested(boolean restockRequested) { this.restockRequested = restockRequested; }
}
