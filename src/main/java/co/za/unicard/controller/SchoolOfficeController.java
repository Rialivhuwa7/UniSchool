package co.za.unicard.controller;

import co.za.unicard.model.Campus;
import co.za.unicard.service.CollectionAdminService;
import co.za.unicard.service.GroupService;
import co.za.unicard.service.SchoolOfficeService;
import co.za.unicard.dto.GroupDtos.*;
import co.za.unicard.dto.PaymentDtos.*;
import co.za.unicard.dto.SchoolDtos.*;
import co.za.unicard.dto.SetupDtos.TeacherAssignment;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The school office (bursar and admin). */
@RestController
@RequestMapping("/api/school")
@PreAuthorize("hasRole('SCHOOL_OFFICE')")
public class SchoolOfficeController {

    private final SchoolOfficeService office;
    private final CollectionAdminService collections;
    private final GroupService groups;

    public SchoolOfficeController(SchoolOfficeService office, CollectionAdminService collections, GroupService groups) {
        this.office = office;
        this.collections = collections;
        this.groups = groups;
    }

    @GetMapping("/overview")
    public Overview overview() { return office.overview(); }

    // ---- collections ----
    @GetMapping("/collections")
    public List<CollectionStats> collections() { return collections.all(); }

    @PostMapping("/collections")
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionStats create(@Valid @RequestBody NewCollectionRequest req) { return collections.create(req); }

    @PostMapping("/collections/{id}/remind")
    public ReminderResult remind(@PathVariable Long id) { return collections.remind(id); }

    @GetMapping(value = "/collections/export", produces = "text/csv")
    public ResponseEntity<String> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"collections.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(collections.csv());
    }

    // ---- access ----
    @GetMapping("/access/requests")
    public List<AccessRequestView> pending() { return office.pendingAccess(); }

    @PostMapping("/access/requests/{id}/approve")
    public Message approve(@PathVariable Long id, @Valid @RequestBody(required = false) TeacherAssignment teaching) {
        return office.approveAccess(id, teaching);
    }

    @PostMapping("/access/requests/{id}/decline")
    public Message decline(@PathVariable Long id) { return office.declineAccess(id); }

    @GetMapping("/access/staff")
    public List<StaffView> staff() { return office.staff(); }

    // ---- groups ----
    @GetMapping("/groups")
    public List<GroupAdminView> groups() { return groups.allGroups(); }

    @PutMapping("/groups/{id}/mode")
    public GroupAdminView mode(@PathVariable Long id, @Valid @RequestBody ModeRequest req) { return groups.setMode(id, req.mode()); }

    @GetMapping("/groups/reported")
    public List<ReportedMessage> reported() { return groups.reported(); }

    @PostMapping("/groups/messages/{messageId}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hide(@PathVariable Long messageId) { groups.hide(messageId); }

    // ---- fees ----
    @GetMapping("/fees")
    public FeePlanBreakdown fees() { return office.feePlans(); }

    @PostMapping("/fees/remind")
    public Message nudge() { return office.nudgeFees(); }

    // ---- supply lists ----
    @GetMapping("/lists")
    public List<ListStatus> lists() { return office.listStatus(); }

    @PutMapping("/lists/{campus}/{grade}")
    public ListStatus saveList(@PathVariable Campus campus, @PathVariable String grade, @Valid @RequestBody PublishListRequest req) {
        return office.saveList(campus, grade, req);
    }

    // ---- uniform ----
    @GetMapping("/uniform")
    public UniformView uniform() { return office.uniform(); }

    @PostMapping("/uniform/stock/{id}/restock")
    public Message restock(@PathVariable Long id) { return office.requestRestock(id); }

    // ---- payouts ----
    @GetMapping("/payouts")
    public List<Payout> payouts() { return office.payouts(); }
}
