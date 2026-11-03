package co.za.unicard.controller;

import co.za.unicard.service.PosService;
import co.za.unicard.dto.TuckshopDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The tuckshop operator's till. */
@RestController
@RequestMapping("/api/pos")
@PreAuthorize("hasRole('TUCKSHOP')")
public class PosController {

    private final PosService pos;

    public PosController(PosService pos) { this.pos = pos; }

    @GetMapping("/today")
    public TillSummary today() {
        return pos.today();
    }

    @GetMapping("/learners")
    public List<TillLearnerView> search(@RequestParam String q) {
        return pos.search(q);
    }

    @GetMapping("/learners/{id}")
    public TillLearnerView learner(@PathVariable Long id) {
        return pos.learner(id);
    }

    @GetMapping("/cards/{token}")
    public TillLearnerView byCard(@PathVariable String token) {
        return pos.byCard(token);
    }

    /** Charge a sale. Declines come back as 200 with approved=false and nothing charged. */
    @PostMapping("/charge")
    public ChargeResult charge(@Valid @RequestBody ChargeRequest req) {
        return pos.charge(req);
    }

    @PostMapping("/pre-orders/{orderId}/handout")
    public HandoutResult handout(@PathVariable Long orderId, @RequestBody(required = false) HandoutRequest req) {
        return pos.handout(orderId, req);
    }
}
