package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** A sport kit bundle to buy, or equipment to rent per month (rent counts towards buying). */
@Entity
@Table(name = "sport_offers")
public class SportOffer {

    public enum Type { BUY, RENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    protected SportOffer() {}

    public SportOffer(String sport, Type type, String name, BigDecimal price) {
        this.sport = sport;
        this.type = type;
        this.name = name;
        this.price = price;
    }

    public String season() {
        return switch (sport) {
            case "Hockey", "Rugby", "Netball" -> "Winter season";
            default -> "Summer season";
        };
    }

    public Long getId() { return id; }
    public String getSport() { return sport; }
    public Type getType() { return type; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
