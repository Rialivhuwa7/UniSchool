package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "shop_order_lines")
public class ShopOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private ShopOrder order;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    private Learner learner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    private int quantity;
    private String size;
    private String note;

    protected ShopOrderLine() {}

    public ShopOrderLine(CartItem c) {
        this.learner = c.getLearner();
        this.category = c.getCategory();
        this.name = c.getName();
        this.unitPrice = c.getUnitPrice();
        this.quantity = c.getQuantity();
        this.size = c.getSize();
        this.note = c.getNote();
    }

    void attach(ShopOrder order) { this.order = order; }

    public Long getId() { return id; }
    public Learner getLearner() { return learner; }
    public ProductCategory getCategory() { return category; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public String getSize() { return size; }
    public String getNote() { return note; }
}
