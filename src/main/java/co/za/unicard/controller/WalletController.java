package co.za.unicard.controller;

import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.ParentWalletService;
import co.za.unicard.dto.LearnerDtos.AmountRequest;
import co.za.unicard.dto.LearnerDtos.TxView;
import co.za.unicard.service.ParentWalletService.WalletView;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@PreAuthorize("hasRole('PARENT')")
public class WalletController {

    private final ParentWalletService service;
    private final CurrentUser currentUser;

    public WalletController(ParentWalletService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public WalletView wallet() {
        return service.wallet(currentUser.get());
    }

    @PostMapping("/top-up")
    public WalletView topUp(@Valid @RequestBody AmountRequest req) {
        return service.topUp(currentUser.get(), req.amount());
    }

    @PostMapping("/move/{learnerId}")
    public WalletView move(@PathVariable Long learnerId, @Valid @RequestBody AmountRequest req) {
        return service.moveToChild(currentUser.get(), learnerId, req.amount());
    }

    @GetMapping("/activity")
    public List<TxView> activity() {
        return service.activity(currentUser.get());
    }
}
