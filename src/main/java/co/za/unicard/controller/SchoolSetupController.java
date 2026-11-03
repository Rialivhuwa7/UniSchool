package co.za.unicard.controller;

import co.za.unicard.dto.GroupDtos.GroupAdminView;
import co.za.unicard.dto.SchoolDtos.Message;
import co.za.unicard.dto.SetupDtos.*;
import co.za.unicard.model.Campus;
import co.za.unicard.service.SchoolSetupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The school office setting the school up: learners, fees, uniform shop, sport kit, groups and lists. */
@RestController
@RequestMapping("/api/school")
@PreAuthorize("hasRole('SCHOOL_OFFICE')")
public class SchoolSetupController {

    private final SchoolSetupService setup;

    public SchoolSetupController(SchoolSetupService setup) {
        this.setup = setup;
    }

    // ---- learners ----
    @GetMapping("/learners")
    public List<EnrolledLearner> learners(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) Campus campus,
                                          @RequestParam(required = false) String grade) {
        return setup.learners(q, campus, grade);
    }

    @PostMapping("/learners")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrolledLearner enrol(@Valid @RequestBody EnrolLearnerRequest req) { return setup.enrol(req); }

    @PutMapping("/learners/{id}")
    public EnrolledLearner update(@PathVariable Long id, @Valid @RequestBody UpdateLearnerRequest req) { return setup.update(id, req); }

    // ---- fees ----
    @GetMapping("/fees/schedule")
    public FeeSchedule feeSchedule() { return setup.feeSchedule(); }

    @PostMapping("/fees/load")
    public Message loadFees(@Valid @RequestBody LoadFeesRequest req) { return setup.loadFees(req); }

    // ---- uniform shop ----
    @GetMapping("/uniform/items")
    public List<UniformItemView> uniformItems() { return setup.uniformItems(); }

    @PostMapping("/uniform/items")
    @ResponseStatus(HttpStatus.CREATED)
    public UniformItemView addUniformItem(@Valid @RequestBody UniformItemRequest req) { return setup.addUniformItem(req); }

    @PatchMapping("/uniform/items/{id}")
    public UniformItemView priceUniformItem(@PathVariable Long id, @Valid @RequestBody PriceRequest req) {
        return setup.priceUniformItem(id, req);
    }

    @PostMapping("/uniform/stock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addStock(@Valid @RequestBody StockRequest req) { setup.addStock(req); }

    @PutMapping("/uniform/stock/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setStock(@PathVariable Long id, @Valid @RequestBody QuantityRequest req) { setup.setStock(id, req); }

    // ---- sport kit ----
    @GetMapping("/sport")
    public List<SportOfferView> sportOffers() { return setup.sportOffers(); }

    @PostMapping("/sport")
    @ResponseStatus(HttpStatus.CREATED)
    public SportOfferView addSportOffer(@Valid @RequestBody SportOfferRequest req) { return setup.addSportOffer(req); }

    @PatchMapping("/sport/{id}")
    public SportOfferView priceSportOffer(@PathVariable Long id, @Valid @RequestBody PriceRequest req) {
        return setup.priceSportOffer(id, req);
    }

    // ---- parent groups ----
    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupAdminView createGroup(@Valid @RequestBody NewGroupRequest req) { return setup.createGroup(req); }

    // ---- stationery lists ----
    @GetMapping("/lists/{campus}/{grade}")
    public SupplyListDetail supplyList(@PathVariable Campus campus, @PathVariable String grade) { return setup.supplyList(campus, grade); }
}
