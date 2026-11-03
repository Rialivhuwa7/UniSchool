package co.za.unicard.controller;

import co.za.unicard.dto.SetupDtos.MenuItemAdminView;
import co.za.unicard.dto.SetupDtos.MenuItemRequest;
import co.za.unicard.dto.SetupDtos.MenuItemUpdate;
import co.za.unicard.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The tuckshop managing its menu: add items, change prices, take items off the menu. */
@RestController
@RequestMapping("/api/pos/menu")
@PreAuthorize("hasRole('TUCKSHOP')")
public class MenuController {

    private final MenuService menu;

    public MenuController(MenuService menu) {
        this.menu = menu;
    }

    @GetMapping
    public List<MenuItemAdminView> all() { return menu.all(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemAdminView add(@Valid @RequestBody MenuItemRequest req) { return menu.add(req); }

    @PatchMapping("/{id}")
    public MenuItemAdminView update(@PathVariable Long id, @Valid @RequestBody MenuItemUpdate req) { return menu.update(id, req); }
}
