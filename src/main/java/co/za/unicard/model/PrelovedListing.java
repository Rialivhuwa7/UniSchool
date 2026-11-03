package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Second-hand uniform or sport kit sold between families at the same school. Payment is held until it arrives. */
@Entity
@Table(name = "preloved_listings")
public class PrelovedListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Campus campus;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Price new, to show the saving. */
    @Column(precision = 10, scale = 2)
    private BigDecimal priceNew;

    /** Size type (Blazer, Jersey, Shoes...) and size, used to tell parents whether it fits. */
    private String sizeType;
    private String size;

    /** For sport equipment: which sport it is for. */
    private String sport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_account_id")
    private UserAccount seller;

    /** How other parents see the seller, e.g. "Parent, Grade 6". */
    private String sellerLabel;

    private boolean sold;

    /** Optimistic locking, so two parents can't both buy the same item. */
    @Version
    private Long version;
    private LocalDateTime listedAt;

    protected PrelovedListing() {}

    public PrelovedListing(Campus campus, String name, BigDecimal price, BigDecimal priceNew, String sizeType, String size,
                           String sport, UserAccount seller, String sellerLabel, LocalDateTime listedAt) {
        this.campus = campus;
        this.name = name;
        this.price = price;
        this.priceNew = priceNew;
        this.sizeType = sizeType;
        this.size = size;
        this.sport = sport;
        this.seller = seller;
        this.sellerLabel = sellerLabel;
        this.listedAt = listedAt;
    }

    public boolean isSoldBy(Long accountId) { return seller != null && seller.getId().equals(accountId); }

    public Long getId() { return id; }
    public Campus getCampus() { return campus; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getPriceNew() { return priceNew; }
    public String getSizeType() { return sizeType; }
    public String getSize() { return size; }
    public String getSport() { return sport; }
    public UserAccount getSeller() { return seller; }
    public String getSellerLabel() { return sellerLabel; }
    public boolean isSold() { return sold; }
    public void markSold() { this.sold = true; }
}
