package co.za.unicard.controller;

import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.LearnerService;
import co.za.unicard.service.LearnerSignInService;
import co.za.unicard.dto.LearnerDtos.*;
import co.za.unicard.dto.SetupDtos.LearnerSignIn;
import co.za.unicard.dto.SetupDtos.LearnerSignInRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class LearnerController {

    private final LearnerService service;
    private final LearnerSignInService signIns;
    private final CurrentUser currentUser;

    public LearnerController(LearnerService service, LearnerSignInService signIns, CurrentUser currentUser) {
        this.service = service;
        this.signIns = signIns;
        this.currentUser = currentUser;
    }

    /** A parent's children (or, for a learner, themselves). */
    @GetMapping("/api/learners")
    @PreAuthorize("hasAnyRole('PARENT','LEARNER')")
    public List<LearnerView> mine() {
        return service.myLearners(currentUser.get());
    }

    @GetMapping("/api/learners/{id}")
    @PreAuthorize("hasAnyRole('PARENT','LEARNER')")
    public LearnerView get(@PathVariable Long id) {
        return service.get(currentUser.get(), id);
    }

    /** Daily limit, freeze card, tuckshop mode, blocked categories, approved items. */
    @PatchMapping("/api/learners/{id}/controls")
    @PreAuthorize("hasRole('PARENT')")
    public LearnerView controls(@PathVariable Long id, @Valid @RequestBody ControlsRequest req) {
        return service.updateControls(currentUser.get(), id, req);
    }

    @GetMapping("/api/learners/{id}/activity")
    @PreAuthorize("hasAnyRole('PARENT','LEARNER')")
    public List<TxView> activity(@PathVariable Long id) {
        return service.activity(currentUser.get(), id);
    }

    /** The child's own sign-in, if the parent has made one. */
    @GetMapping("/api/learners/{id}/sign-in")
    @PreAuthorize("hasRole('PARENT')")
    public LearnerSignIn signIn(@PathVariable Long id) {
        return signIns.get(currentUser.get(), id);
    }

    @PostMapping("/api/learners/{id}/sign-in")
    @PreAuthorize("hasRole('PARENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public LearnerSignIn createSignIn(@PathVariable Long id, @Valid @RequestBody LearnerSignInRequest req) {
        return signIns.create(currentUser.get(), id, req);
    }

    /** The learner's own card: QR token, wallet balance and savings goal. */
    @GetMapping("/api/me/card")
    @PreAuthorize("hasRole('LEARNER')")
    public CardView card() {
        return service.myCard(currentUser.get());
    }

    @PostMapping("/api/me/goal/save")
    @PreAuthorize("hasRole('LEARNER')")
    public LearnerView saveTowardsGoal(@RequestParam(required = false) BigDecimal amount) {
        return service.saveTowardsGoal(currentUser.get(), amount);
    }
}
