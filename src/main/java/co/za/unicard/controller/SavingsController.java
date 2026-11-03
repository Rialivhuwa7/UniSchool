package co.za.unicard.controller;

import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.SavingsService;
import co.za.unicard.dto.LearnerDtos.AmountRequest;
import co.za.unicard.dto.SavingsDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SavingsController {

    private final SavingsService service;
    private final CurrentUser currentUser;

    public SavingsController(SavingsService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/savings")
    @PreAuthorize("hasRole('PARENT')")
    public PotView pot() {
        return service.pot(currentUser.get());
    }

    @PostMapping("/api/savings/add")
    @PreAuthorize("hasRole('PARENT')")
    public PotView add(@Valid @RequestBody AmountRequest req) {
        return service.addNow(currentUser.get(), req.amount());
    }

    @PutMapping("/api/savings/monthly")
    @PreAuthorize("hasRole('PARENT')")
    public PotView monthly(@Valid @RequestBody MonthlyRequest req) {
        return service.setMonthly(currentUser.get(), req.monthlyDebit());
    }

    @GetMapping("/api/pocket-money")
    @PreAuthorize("hasRole('PARENT')")
    public List<PocketMoneyView> pocketMoney() {
        return service.pocketMoney(currentUser.get());
    }

    @PostMapping("/api/pocket-money")
    @PreAuthorize("hasRole('PARENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public PocketMoneyView addPocketMoney(@Valid @RequestBody PocketMoneyRequest req) {
        return service.addPocketMoney(currentUser.get(), req);
    }

    @PatchMapping("/api/pocket-money/{id}")
    @PreAuthorize("hasRole('PARENT')")
    public PocketMoneyView updatePocketMoney(@PathVariable Long id, @Valid @RequestBody PocketMoneyUpdate req) {
        return service.updatePocketMoney(currentUser.get(), id, req);
    }

    // ---- public share link for grandparents and relatives (no sign-in) ----
    @GetMapping("/api/give/{token}")
    public GivePage givePage(@PathVariable String token) {
        return service.givePage(token);
    }

    @PostMapping("/api/give/{token}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void give(@PathVariable String token, @Valid @RequestBody GiveRequest req) {
        service.give(token, req);
    }
}
