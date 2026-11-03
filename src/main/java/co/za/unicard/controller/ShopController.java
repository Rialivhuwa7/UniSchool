package co.za.unicard.controller;

import co.za.unicard.model.ProductCategory;
import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.ShopService;
import co.za.unicard.dto.ShopDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop")
@PreAuthorize("hasRole('PARENT')")
public class ShopController {

    private final ShopService shop;
    private final CurrentUser currentUser;

    public ShopController(ShopService shop, CurrentUser currentUser) {
        this.shop = shop;
        this.currentUser = currentUser;
    }

    /** Products tailored to one child; optional ?category=UNIFORM|SPORT|PRELOVED|LABELS */
    @GetMapping("/catalog")
    public List<CatalogEntry> catalog(@RequestParam Long learnerId, @RequestParam(required = false) ProductCategory category) {
        return shop.catalog(currentUser.get(), learnerId, category);
    }

    @GetMapping("/supply-list")
    public SupplyListView supplyList(@RequestParam Long learnerId) {
        return shop.supplyList(currentUser.get(), learnerId);
    }

    @GetMapping("/cart")
    public CartView cart() {
        return shop.cart(currentUser.get());
    }

    @PostMapping("/cart")
    public CartView add(@Valid @RequestBody AddToCartRequest req) {
        return shop.addToCart(currentUser.get(), req);
    }

    @PostMapping("/cart/supply-list")
    public CartView addSupplyList(@Valid @RequestBody AddSupplyListRequest req) {
        return shop.addSupplyList(currentUser.get(), req);
    }

    @DeleteMapping("/cart/{itemId}")
    public CartView remove(@PathVariable Long itemId) {
        return shop.removeFromCart(currentUser.get(), itemId);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView checkout(@Valid @RequestBody CheckoutRequest req) {
        return shop.checkout(currentUser.get(), req);
    }

    @GetMapping("/orders")
    public List<OrderView> orders() {
        return shop.orders(currentUser.get());
    }

    /** List a pre-loved item for other families at the same school. */
    @PostMapping("/preloved")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogEntry sell(@Valid @RequestBody SellRequest req) {
        return shop.sell(currentUser.get(), req);
    }

    @DeleteMapping("/preloved/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlist(@PathVariable Long id) {
        shop.unlist(currentUser.get(), id);
    }
}
