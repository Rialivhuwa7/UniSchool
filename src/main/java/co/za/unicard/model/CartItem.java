package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** A line in a parent's cart, always for one of their children. */
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private UserAccount account;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id")
    private Learner learner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category;

    /** Catalogue key, e.g. "u:12", "s:3", "p:7", "l:IRON_ON", "list:5". */
    @Column(nullable = false)
    private String productKey;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    private int quantity = 1;
    private String size;
    private String note;
    private Long prelovedId;

    protected CartItem() {}

    public CartItem(UserAccount account, Learner learner, ProductCategory category, String productKey, String name,
                    BigDecimal unitPrice, int quantity, String size, String note, Long prelovedId) {
        this.account = account;
        this.learner = learner;
        this.category = category;
        this.productKey = productKey;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.size = size;
        this.note = note;
        this.prelovedId = prelovedId;
    }

    public BigDecimal lineTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }

    public Long getId() { return id; }
    public UserAccount getAccount() { return account; }
    public Learner getLearner() { return learner; }
    public ProductCategory getCategory() { return category; }
    public String getProductKey() { return productKey; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public String getSize() { return size; }
    public String getNote() { return note; }
    public Long getPrelovedId() { return prelovedId; }
}
