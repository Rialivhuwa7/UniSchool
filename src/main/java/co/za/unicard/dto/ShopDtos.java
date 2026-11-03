package co.za.unicard.dto;

import co.za.unicard.model.Campus;
import co.za.unicard.model.CartItem;
import co.za.unicard.model.ProductCategory;
import co.za.unicard.model.ShopOrder;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class ShopDtos {
    private ShopDtos() {}

    /** One product card in the shop, already tailored to a learner (sizes, fit, campus). */
    public record CatalogEntry(String key, ProductCategory category, String name, String description, BigDecimal price,
                               boolean perMonth, BigDecimal priceNew, String size, List<String> badges,
                               Boolean fits, boolean mine, boolean inCart) {}

    public record SupplyListLine(Long itemId, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    public record SupplyListView(Long learnerId, String learnerName, String grade, Campus campus, boolean published,
                                 List<SupplyListLine> items, BigDecimal fullListTotal, int fullListDiscountPercent,
                                 boolean inCart) {}

    public record AddToCartRequest(@NotNull Long learnerId, @NotBlank String key) {}

    /** Add the learner's supply list, leaving out items the family already has. */
    public record AddSupplyListRequest(@NotNull Long learnerId, Set<Long> alreadyHaveItemIds) {}

    public record CartLine(Long id, Long learnerId, String learnerName, ProductCategory category, String name,
                           BigDecimal unitPrice, int quantity, String size, String note, BigDecimal lineTotal) {
        public static CartLine of(CartItem c) {
            return new CartLine(c.getId(), c.getLearner().getId(), c.getLearner().getFirstName(), c.getCategory(),
                    c.getName(), c.getUnitPrice(), c.getQuantity(), c.getSize(), c.getNote(), c.lineTotal());
        }
    }

    public record CartView(List<CartLine> items, int count, BigDecimal subtotal, BigDecimal homeDeliveryFee,
                           BigDecimal freeHomeDeliveryFrom) {}

    public record CheckoutRequest(@NotNull ShopOrder.Delivery delivery, @NotNull ShopOrder.PaymentMethod paymentMethod) {}

    public record OrderLineView(Long learnerId, String learnerName, ProductCategory category, String name,
                                BigDecimal unitPrice, int quantity, String size, String note) {}

    public record OrderView(Long id, String orderNumber, LocalDateTime placedAt, BigDecimal subtotal, BigDecimal deliveryFee,
                            BigDecimal total, ShopOrder.Delivery delivery, ShopOrder.PaymentMethod paymentMethod,
                            BigDecimal schoolGiveBack, List<OrderLineView> lines) {
        public static OrderView of(ShopOrder o, BigDecimal giveBack) {
            return new OrderView(o.getId(), o.getOrderNumber(), o.getPlacedAt(), o.getSubtotal(), o.getDeliveryFee(),
                    o.getTotal(), o.getDelivery(), o.getPaymentMethod(), giveBack,
                    o.getLines().stream().map(l -> new OrderLineView(l.getLearner().getId(), l.getLearner().getFirstName(),
                            l.getCategory(), l.getName(), l.getUnitPrice(), l.getQuantity(), l.getSize(), l.getNote())).toList());
        }
    }

    public record SellRequest(@NotNull Long learnerId, @NotBlank @Size(max = 120) String name,
                              @NotNull @DecimalMin(value = "10.00", message = "Set a price of at least R10.") BigDecimal price,
                              BigDecimal priceNew, String sizeType, String size, String sport) {}
}
