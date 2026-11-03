package co.za.unicard.service;

import co.za.unicard.model.LabelProduct;
import co.za.unicard.model.Learner;
import co.za.unicard.model.PrelovedListing;
import co.za.unicard.model.ProductCategory;
import co.za.unicard.model.SportOffer;
import co.za.unicard.model.UniformItem;
import co.za.unicard.repository.PrelovedListingRepository;
import co.za.unicard.repository.SportOfferRepository;
import co.za.unicard.repository.UniformItemRepository;
import co.za.unicard.dto.ShopDtos.CatalogEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builds the shop for one learner: their campus's uniform in their sizes, kit for the sports
 * they play, pre-loved items from families at the same school (flagged when they fit), and
 * name labels. Also resolves a catalogue key back into a cart line.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    public record Resolved(ProductCategory category, String name, java.math.BigDecimal price, String size, String note, Long prelovedId) {}

    private final UniformItemRepository uniforms;
    private final SportOfferRepository sport;
    private final PrelovedListingRepository preloved;

    public CatalogService(UniformItemRepository uniforms, SportOfferRepository sport, PrelovedListingRepository preloved) {
        this.uniforms = uniforms;
        this.sport = sport;
        this.preloved = preloved;
    }

    public List<CatalogEntry> catalog(Learner c, Long viewerAccountId, Set<String> keysInCart) {
        List<CatalogEntry> out = new ArrayList<>();
        for (UniformItem u : uniforms.findByCampusOrderByIdAsc(c.getCampus())) {
            String size = u.getSizeType() == null ? null : c.getSizes().get(u.getSizeType());
            List<String> badges = new ArrayList<>(List.of(u.getKind() == UniformItem.Kind.CRESTED ? "Crested" : "Generic", u.season()));
            if (size != null) badges.add("Size " + size);
            String key = "u:" + u.getId();
            out.add(new CatalogEntry(key, ProductCategory.UNIFORM, u.getName(),
                    u.getKind() == UniformItem.Kind.CRESTED ? "Approved supplier: " + u.getSupplier() : "Generic. Any matching item is allowed.",
                    u.getPrice(), false, null, size, badges, null, false, keysInCart.contains(key)));
        }
        if (!c.getSports().isEmpty()) {
            for (SportOffer s : sport.findBySportInOrderByIdAsc(c.getSports())) {
                boolean rent = s.getType() == SportOffer.Type.RENT;
                String key = "s:" + s.getId();
                out.add(new CatalogEntry(key, ProductCategory.SPORT, rent ? "Rent: " + s.getName() : s.getName(),
                        rent ? "Rent counts towards buying it. Trade up as " + c.getFirstName() + " grows."
                             : "In " + c.getCampus().displayName() + " colours, sized for " + c.getFirstName() + ".",
                        s.getPrice(), rent, null, null, List.of("For " + s.getSport().toLowerCase(), rent ? "Rental" : s.season()),
                        null, false, keysInCart.contains(key)));
            }
        }
        for (PrelovedListing p : preloved.findByCampusAndSoldFalseOrderByListedAtDesc(c.getCampus())) {
            boolean mine = p.isSoldBy(viewerAccountId);
            boolean fits = fits(p, c);
            List<String> badges = new ArrayList<>();
            if (fits && !mine) badges.add("Fits " + c.getFirstName());
            if (p.getPriceNew() != null && p.getPriceNew().compareTo(p.getPrice()) > 0) {
                badges.add("Save R" + p.getPriceNew().subtract(p.getPrice()).setScale(0, java.math.RoundingMode.HALF_UP));
            }
            if (mine) badges.add("Your listing");
            String key = "p:" + p.getId();
            out.add(new CatalogEntry(key, ProductCategory.PRELOVED, p.getName(),
                    (mine ? "Your listing" : p.getSellerLabel()) + ". Payment held until it arrives.",
                    p.getPrice(), false, p.getPriceNew(), p.getSize(), badges, fits, mine, keysInCart.contains(key)));
        }
        for (LabelProduct l : LabelProduct.values()) {
            String key = "l:" + l.name();
            out.add(new CatalogEntry(key, ProductCategory.LABELS, l.title(), l.description(), l.price(), false, null, null,
                    List.of(c.getFullName()), null, false, keysInCart.contains(key)));
        }
        return out;
    }

    /** Turns "u:12" etc. into a cart line for this learner, or fails if it isn't for them. */
    public Resolved resolve(Learner c, String key, Long viewerAccountId) {
        String[] parts = key.split(":", 2);
        if (parts.length != 2) throw new IllegalArgumentException("Unknown product " + key);
        switch (parts[0]) {
            case "u" -> {
                UniformItem u = uniforms.findById(parseId(parts[1]))
                        .filter(x -> x.getCampus() == c.getCampus())
                        .orElseThrow(() -> new IllegalArgumentException("That uniform item isn't for " + c.getCampus().displayName()));
                String size = u.getSizeType() == null ? null : c.getSizes().get(u.getSizeType());
                return new Resolved(ProductCategory.UNIFORM, u.getName(), u.getPrice(), size, null, null);
            }
            case "s" -> {
                SportOffer s = sport.findById(parseId(parts[1]))
                        .filter(x -> c.getSports().contains(x.getSport()))
                        .orElseThrow(() -> new IllegalArgumentException(c.getFirstName() + " doesn't play that sport."));
                boolean rent = s.getType() == SportOffer.Type.RENT;
                return new Resolved(ProductCategory.SPORT, (rent ? "Rental: " : "") + s.getName(), s.getPrice(), null,
                        rent ? "first month" : null, null);
            }
            case "p" -> {
                PrelovedListing p = preloved.findById(parseId(parts[1]))
                        .filter(x -> x.getCampus() == c.getCampus() && !x.isSold())
                        .orElseThrow(() -> new IllegalArgumentException("That item has been sold."));
                if (p.isSoldBy(viewerAccountId)) throw new IllegalArgumentException("You can't buy your own listing.");
                return new Resolved(ProductCategory.PRELOVED, p.getName(), p.getPrice(), p.getSize(), "held until it arrives", p.getId());
            }
            case "l" -> {
                LabelProduct l;
                try { l = LabelProduct.valueOf(parts[1]); }
                catch (IllegalArgumentException e) { throw new IllegalArgumentException("Unknown label product " + parts[1]); }
                return new Resolved(ProductCategory.LABELS, l.title(), l.price(), null, c.getFullName(), null);
            }
            default -> throw new IllegalArgumentException("Unknown product " + key);
        }
    }

    static boolean fits(PrelovedListing p, Learner c) {
        if (p.getSizeType() != null) return Objects.equals(c.getSizes().get(p.getSizeType()), p.getSize());
        if (p.getSport() != null) return c.getSports().contains(p.getSport());
        return true;
    }

    private static Long parseId(String s) {
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Unknown product id " + s); }
    }
}
