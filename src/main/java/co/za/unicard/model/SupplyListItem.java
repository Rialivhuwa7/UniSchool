package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "supply_list_items")
public class SupplyListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id")
    private SupplyList list;

    @Column(nullable = false)
    private String name;

    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    private int position;

    protected SupplyListItem() {}

    public SupplyListItem(String name, int quantity, BigDecimal unitPrice) {
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    void attach(SupplyList list, int position) {
        this.list = list;
        this.position = position;
    }

    public BigDecimal lineTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
