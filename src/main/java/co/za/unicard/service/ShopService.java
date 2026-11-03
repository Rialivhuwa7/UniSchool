package co.za.unicard.service;

import co.za.unicard.exception.BusinessRuleException;
import co.za.unicard.exception.ConflictException;
import co.za.unicard.exception.ForbiddenException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.CartItem;
import co.za.unicard.model.Learner;
import co.za.unicard.model.LedgerSource;
import co.za.unicard.model.PrelovedListing;
import co.za.unicard.model.ProductCategory;
import co.za.unicard.model.SavingsPot;
import co.za.unicard.model.SchoolLedgerEntry;
import co.za.unicard.model.ShopOrder;
import co.za.unicard.model.ShopOrderLine;
import co.za.unicard.model.SupplyList;
import co.za.unicard.model.SupplyListItem;
import co.za.unicard.model.TxType;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.CartItemRepository;
import co.za.unicard.repository.PrelovedListingRepository;
import co.za.unicard.repository.SavingsPotRepository;
import co.za.unicard.repository.SchoolLedgerRepository;
import co.za.unicard.repository.ShopOrderRepository;
import co.za.unicard.repository.SupplyListRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.util.Money;
import co.za.unicard.dto.ShopDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Parent shopping: catalogue, supply lists, cart, checkout, orders and pre-loved listings. */
@Service
@Transactional
public class ShopService {

    public static final BigDecimal HOME_DELIVERY_FEE = Money.of(65);
    public static final BigDecimal FREE_DELIVERY_FROM = Money.of(750);
    public static final int SCHOOL_GIVEBACK_PERCENT = 5;
    public static final int FULL_LIST_DISCOUNT_PERCENT = 5;

    private final CatalogService catalog;
    private final CartItemRepository cart;
    private final SupplyListRepository lists;
    private final ShopOrderRepository orders;
    private final PrelovedListingRepository preloved;
    private final SavingsPotRepository pots;
    private final SchoolLedgerRepository ledger;
    private final LearnerAccess access;
    private final WalletService wallets;
    private final SchoolCalendar calendar;

    public ShopService(CatalogService catalog, CartItemRepository cart, SupplyListRepository lists, ShopOrderRepository orders,
                       PrelovedListingRepository preloved, SavingsPotRepository pots, SchoolLedgerRepository ledger,
                       LearnerAccess access, WalletService wallets, SchoolCalendar calendar) {
        this.catalog = catalog;
        this.cart = cart;
        this.lists = lists;
        this.orders = orders;
        this.preloved = preloved;
        this.pots = pots;
        this.ledger = ledger;
        this.access = access;
        this.wallets = wallets;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public List<CatalogEntry> catalog(Actor actor, Long learnerId, ProductCategory category) {
        Learner l = access.requireAsParent(actor, learnerId);
        Set<String> inCart = cart.findByAccount(actor.accountId()).stream()
                .filter(c -> c.getLearner().getId().equals(learnerId))
                .map(CartItem::getProductKey).collect(Collectors.toSet());
        return catalog.catalog(l, actor.accountId(), inCart).stream()
                .filter(e -> category == null || e.category() == category).toList();
    }

    @Transactional(readOnly = true)
    public SupplyListView supplyList(Actor actor, Long learnerId) {
        Learner l = access.requireAsParent(actor, learnerId);
        SupplyList list = publishedList(l);
        List<SupplyListLine> lines = list.getItems().stream()
                .map(i -> new SupplyListLine(i.getId(), i.getName(), i.getQuantity(), i.getUnitPrice(), i.lineTotal())).toList();
        BigDecimal total = lines.stream().map(SupplyListLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean inCart = cart.findByAccount(actor.accountId()).stream()
                .anyMatch(c -> c.getLearner().getId().equals(learnerId) && c.getCategory() == ProductCategory.SUPPLY_LIST);
        return new SupplyListView(l.getId(), l.getFirstName(), l.getGrade(), l.getCampus(), true, lines,
                Money.round(total), FULL_LIST_DISCOUNT_PERCENT, inCart);
    }

    public CartView addToCart(Actor actor, AddToCartRequest req) {
        Learner l = access.requireAsParent(actor, req.learnerId());
        boolean dup = cart.findByAccount(actor.accountId()).stream()
                .anyMatch(c -> c.getLearner().getId().equals(l.getId()) && c.getProductKey().equals(req.key()));
        if (dup) throw new ConflictException("Already in your cart.");
        CatalogService.Resolved r = catalog.resolve(l, req.key(), actor.accountId());
        if (r.prelovedId() != null && cart.findByAccount(actor.accountId()).stream().anyMatch(c -> r.prelovedId().equals(c.getPrelovedId()))) {
            throw new ConflictException("Already in your cart.");
        }
        cart.save(new CartItem(wallets.managed(actor.accountId()), l, r.category(), req.key(), r.name(), r.price(), 1,
                r.size(), r.note(), r.prelovedId()));
        return cartView(actor);
    }

    /**
     * Adds the learner's supply list, minus items the family already has. If they take the
     * full list it is 5% cheaper. Replaces any list already in the cart for that learner.
     */
    public CartView addSupplyList(Actor actor, AddSupplyListRequest req) {
        Learner l = access.requireAsParent(actor, req.learnerId());
        SupplyList list = publishedList(l);
        Set<Long> have = req.alreadyHaveItemIds() == null ? Set.of() : req.alreadyHaveItemIds();
        cart.findByAccount(actor.accountId()).stream()
                .filter(c -> c.getLearner().getId().equals(l.getId()) && c.getCategory() == ProductCategory.SUPPLY_LIST)
                .forEach(cart::delete);
        boolean full = list.getItems().stream().noneMatch(i -> have.contains(i.getId()));
        UserAccount account = wallets.managed(actor.accountId());
        int added = 0;
        for (SupplyListItem i : list.getItems()) {
            if (have.contains(i.getId())) continue;
            BigDecimal price = full
                    ? Money.round(i.getUnitPrice().multiply(BigDecimal.valueOf(100 - FULL_LIST_DISCOUNT_PERCENT)).movePointLeft(2))
                    : i.getUnitPrice();
            cart.save(new CartItem(account, l, ProductCategory.SUPPLY_LIST, "list:" + list.getId(), i.getName(), price,
                    i.getQuantity(), null, full ? "full list, 5% off" : null, null));
            added++;
        }
        if (added == 0) throw new IllegalArgumentException("You already have everything on the list.");
        return cartView(actor);
    }

    @Transactional(readOnly = true)
    public CartView cart(Actor actor) {
        return cartView(actor);
    }

    public CartView removeFromCart(Actor actor, Long cartItemId) {
        CartItem c = cart.findById(cartItemId)
                .filter(x -> x.getAccount().getId().equals(actor.accountId()))
                .orElseThrow(() -> new NotFoundException("Cart item", cartItemId));
        cart.delete(c);
        return cartView(actor);
    }

    public OrderView checkout(Actor actor, CheckoutRequest req) {
        List<CartItem> items = cart.findByAccount(actor.accountId());
        if (items.isEmpty()) throw new IllegalArgumentException("Your cart is empty.");
        UserAccount account = wallets.managed(actor.accountId());

        // Pre-loved items may have been bought by someone else since they were added.
        for (CartItem c : items) {
            if (c.getPrelovedId() != null) {
                PrelovedListing p = preloved.findById(c.getPrelovedId()).orElse(null);
                if (p == null || p.isSold()) throw new ConflictException(c.getName() + " has just been sold. Remove it to continue.");
            }
        }

        BigDecimal subtotal = Money.round(items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal delivery = req.delivery() == ShopOrder.Delivery.TO_HOME && subtotal.compareTo(FREE_DELIVERY_FROM) < 0
                ? HOME_DELIVERY_FEE : Money.ZERO;
        BigDecimal total = subtotal.add(delivery);

        ShopOrder order = new ShopOrder(account, calendar.now(), subtotal, delivery, req.delivery(), req.paymentMethod());
        items.forEach(c -> order.addLine(new ShopOrderLine(c)));
        orders.saveAndFlush(order);
        order.assignNumber();

        switch (req.paymentMethod()) {
            case WALLET -> wallets.debitAccount(account, total, TxType.SHOP_ORDER, "UniSchool order " + order.getOrderNumber(), null);
            case SAVINGS -> {
                SavingsPot pot = pots.findByAccountId(account.getId())
                        .orElseThrow(() -> new BusinessRuleException("You don't have a savings pot yet."));
                if (Money.lessThan(pot.getBalance(), total)) {
                    throw new BusinessRuleException("The savings pot has " + Money.format(pot.getBalance()) + ". Choose another way to pay.");
                }
                pot.withdraw(total);
            }
            case CARD, PAY_IN_4 -> { /* handled by the payment provider before this call in production */ }
        }

        for (CartItem c : items) {
            if (c.getPrelovedId() != null) preloved.findById(c.getPrelovedId()).ifPresent(PrelovedListing::markSold);
        }
        BigDecimal giveBack = Money.percentOf(subtotal, SCHOOL_GIVEBACK_PERCENT);
        ledger.save(new SchoolLedgerEntry(calendar.now(), "UniSchool give-back, order " + order.getOrderNumber(), giveBack,
                null, LedgerSource.SHOP_GIVEBACK));
        cart.deleteAll(items);
        return OrderView.of(order, giveBack);
    }

    @Transactional(readOnly = true)
    public List<OrderView> orders(Actor actor) {
        return orders.findByAccountIdOrderByPlacedAtDesc(actor.accountId()).stream()
                .map(o -> OrderView.of(o, Money.percentOf(o.getSubtotal(), SCHOOL_GIVEBACK_PERCENT))).toList();
    }

    public CatalogEntry sell(Actor actor, SellRequest req) {
        Learner l = access.requireAsParent(actor, req.learnerId());
        PrelovedListing p = preloved.save(new PrelovedListing(l.getCampus(), req.name().trim(), Money.round(req.price()),
                req.priceNew() == null ? null : Money.round(req.priceNew()), req.sizeType(), req.size(), req.sport(),
                wallets.managed(actor.accountId()), "Parent, " + l.getGrade(), calendar.now()));
        return new CatalogEntry("p:" + p.getId(), ProductCategory.PRELOVED, p.getName(), "Your listing. Payment held until it arrives.",
                p.getPrice(), false, p.getPriceNew(), p.getSize(), List.of("Your listing"), null, true, false);
    }

    public void unlist(Actor actor, Long listingId) {
        PrelovedListing p = preloved.findById(listingId).orElseThrow(() -> new NotFoundException("Listing", listingId));
        if (!p.isSoldBy(actor.accountId())) throw new ForbiddenException("You can only remove your own listings.");
        if (p.isSold()) throw new ConflictException("This item has already been sold.");
        preloved.delete(p);
    }

    private SupplyList publishedList(Learner l) {
        return lists.findByCampusAndGrade(l.getCampus(), l.getGrade())
                .filter(x -> x.getStatus() == SupplyList.Status.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("The school hasn't published the " + l.getGrade() + " list yet."));
    }

    private CartView cartView(Actor actor) {
        List<CartItem> items = cart.findByAccount(actor.accountId());
        BigDecimal subtotal = Money.round(items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new CartView(items.stream().map(CartLine::of).toList(), items.size(), subtotal, HOME_DELIVERY_FEE, FREE_DELIVERY_FROM);
    }
}
