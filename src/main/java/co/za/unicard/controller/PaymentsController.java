package co.za.unicard.controller;

import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.PaymentService;
import co.za.unicard.dto.PaymentDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Parents: trips, events and levies; tickets; 2027 fee plans. */
@RestController
@PreAuthorize("hasRole('PARENT')")
public class PaymentsController {

    private final PaymentService service;
    private final CurrentUser currentUser;

    public PaymentsController(PaymentService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/payments/collections")
    public List<CollectionForChild> collections() {
        return service.collections(currentUser.get());
    }

    @PostMapping("/api/payments/collections/{id}/pay")
    public PaymentReceipt pay(@PathVariable Long id, @Valid @RequestBody PayRequest req) {
        return service.pay(currentUser.get(), id, req);
    }

    @GetMapping("/api/payments/tickets")
    public List<TicketView> tickets() {
        return service.tickets(currentUser.get());
    }

    @GetMapping("/api/fees")
    public List<FeeView> fees() {
        return service.fees(currentUser.get());
    }

    @PutMapping("/api/fees/{learnerId}/plan")
    public FeeView choosePlan(@PathVariable Long learnerId, @Valid @RequestBody ChoosePlanRequest req) {
        return service.choosePlan(currentUser.get(), learnerId, req.plan());
    }
}
