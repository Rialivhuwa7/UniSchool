package co.za.unicard.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** A uniform item for one campus. Crested items come from an approved supplier; generic ones can be bought anywhere. */
@Entity
@Table(name = "uniform_items")
public class UniformItem {

    public enum Kind { GENERIC, CRESTED }

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    /** Which of the learner's sizes applies: Shirt, Shorts, Trousers, Shoes, Blazer, Jersey; null for one size. */
    private String sizeType;

    private String supplier;

    protected UniformItem() {}

    public UniformItem(Campus campus, String name, BigDecimal price, Kind kind, String sizeType, String supplier) {
        this.campus = campus;
        this.name = name;
        this.price = price;
        this.kind = kind;
        this.sizeType = sizeType;
        this.supplier = supplier;
    }

    /** winter (jerseys, tracksuits), summer (short sleeves, shorts) or all year. */
    public String season() {
        String n = name.toLowerCase();
        if (n.contains("tracksuit") || n.contains("jersey")) return "Winter";
        if (n.contains("short-sleeve") || n.contains("shorts")) return "Summer";
        return "All year";
    }

    public Long getId() { return id; }
    public Campus getCampus() { return campus; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Kind getKind() { return kind; }
    public String getSizeType() { return sizeType; }
    public String getSupplier() { return supplier; }
}
