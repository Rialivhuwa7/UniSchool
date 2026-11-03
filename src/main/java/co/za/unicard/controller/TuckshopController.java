package co.za.unicard.controller;

import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.TuckshopService;
import co.za.unicard.dto.TuckshopDtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Parents and learners: the menu and pre-orders for the week. */
@RestController
@RequestMapping("/api/tuckshop")
public class TuckshopController {

    private final TuckshopService service;
    private final CurrentUser currentUser;

    public TuckshopController(TuckshopService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/menu")
    public List<MenuItemView> menu() {
        return service.menu();
    }

    @GetMapping("/week")
    @PreAuthorize("hasAnyRole('PARENT','LEARNER')")
    public List<LearnerWeek> week() {
        return service.week(currentUser.get());
    }

    /** Place or replace the order for one learner on one day. */
    @PutMapping("/learners/{learnerId}/orders")
    @PreAuthorize("hasAnyRole('PARENT','LEARNER')")
    public PreOrderView place(@PathVariable Long learnerId, @Valid @RequestBody PlaceOrderRequest req) {
        return service.place(currentUser.get(), learnerId, req);
    }

    @DeleteMapping("/learners/{learnerId}/orders/{day}")
    @PreAuthorize("hasAnyRole('PARENT','LEARNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long learnerId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        service.cancel(currentUser.get(), learnerId, day);
    }
}
