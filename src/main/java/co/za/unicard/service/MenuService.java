package co.za.unicard.service;

import co.za.unicard.dto.SetupDtos.MenuItemAdminView;
import co.za.unicard.dto.SetupDtos.MenuItemRequest;
import co.za.unicard.dto.SetupDtos.MenuItemUpdate;
import co.za.unicard.exception.ConflictException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.MenuItem;
import co.za.unicard.repository.MenuItemRepository;
import co.za.unicard.util.Money;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The tuckshop keeps its own menu. Items are never deleted, because pre-orders and parents'
 * approved lists refer to them by name; an item that's off the menu is marked unavailable.
 */
@Service
@Transactional
public class MenuService {

    private final MenuItemRepository menu;

    public MenuService(MenuItemRepository menu) {
        this.menu = menu;
    }

    @Transactional(readOnly = true)
    public List<MenuItemAdminView> all() {
        return menu.findAll(Sort.by("category", "name")).stream().map(this::view).toList();
    }

    public MenuItemAdminView add(MenuItemRequest req) {
        String name = req.name().trim();
        boolean taken = menu.findAll().stream().anyMatch(i -> i.getName().equalsIgnoreCase(name));
        if (taken) throw new ConflictException(name + " is already on the menu. Change it instead.");
        return view(menu.save(new MenuItem(name, Money.round(req.price()), req.category().trim())));
    }

    public MenuItemAdminView update(Long id, MenuItemUpdate req) {
        MenuItem i = menu.findById(id).orElseThrow(() -> new NotFoundException("Menu item", id));
        if (req.price() != null) i.setPrice(Money.round(req.price()));
        if (req.category() != null && !req.category().isBlank()) i.setCategory(req.category().trim());
        if (req.available() != null) i.setAvailable(req.available());
        return view(i);
    }

    private MenuItemAdminView view(MenuItem i) {
        return new MenuItemAdminView(i.getId(), i.getName(), i.getPrice(), i.getCategory(), i.isAvailable());
    }
}
