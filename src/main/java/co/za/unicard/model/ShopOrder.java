package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shop_orders")
public class ShopOrder {

    public enum Delivery { TO_SCHOOL, TO_HOME }
    public enum PaymentMethod { WALLET, SAVINGS, CARD, PAY_IN_4 }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private UserAccount account;

    @Column(nullable = false)
    private LocalDateTime placedAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ShopOrderLine> lines = new ArrayList<>();

    protected ShopOrder() {}

    public ShopOrder(UserAccount account, LocalDateTime placedAt, BigDecimal subtotal, BigDecimal deliveryFee,
                     Delivery delivery, PaymentMethod paymentMethod) {
        this.account = account;
        this.placedAt = placedAt;
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.total = subtotal.add(deliveryFee);
        this.delivery = delivery;
        this.paymentMethod = paymentMethod;
    }

    public void addLine(ShopOrderLine line) {
        line.attach(this);
        lines.add(line);
    }

    public void assignNumber() { this.orderNumber = "US-" + (1040 + id); }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public UserAccount getAccount() { return account; }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public BigDecimal getTotal() { return total; }
    public Delivery getDelivery() { return delivery; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public List<ShopOrderLine> getLines() { return lines; }
}
