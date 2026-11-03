package co.za.unicard.controller;

import co.za.unicard.service.HomeworkService;
import co.za.unicard.service.NoticeService;
import co.za.unicard.service.PrincipalService;
import co.za.unicard.dto.NoticeDtos.*;
import co.za.unicard.dto.SchoolDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/principal")
@PreAuthorize("hasRole('PRINCIPAL')")
public class PrincipalController {

    private final PrincipalService principal;
    private final NoticeService notices;
    private final HomeworkService homework;

    public PrincipalController(PrincipalService principal, NoticeService notices, HomeworkService homework) {
        this.principal = principal;
        this.notices = notices;
        this.homework = homework;
    }

    @GetMapping("/dashboard")
    public PrincipalDashboard dashboard() { return principal.dashboard(); }

    @GetMapping("/approvals")
    public List<ApprovalItem> approvals() { return principal.approvals(); }

    @PostMapping("/approvals/notices/{id}/approve")
    public PostResult approveNotice(@PathVariable Long id) { return notices.approve(id); }

    @PostMapping("/approvals/notices/{id}/request-changes")
    public PostResult noticeChanges(@PathVariable Long id, @Valid @RequestBody RequestChanges req) {
        return notices.requestChanges(id, req.comment());
    }

    @PutMapping("/approvals/notices/{id}")
    public PostResult editNotice(@PathVariable Long id, @Valid @RequestBody EditRequest req) { return notices.editAndApprove(id, req); }

    @PostMapping("/approvals/homework/{id}/approve")
    public PostResult approveHomework(@PathVariable Long id) { return homework.approve(id); }

    @PostMapping("/approvals/homework/{id}/request-changes")
    public PostResult homeworkChanges(@PathVariable Long id, @Valid @RequestBody RequestChanges req) {
        return homework.requestChanges(id, req.comment());
    }

    @PutMapping("/approvals/homework/{id}")
    public PostResult editHomework(@PathVariable Long id, @Valid @RequestBody EditRequest req) { return homework.editAndApprove(id, req); }

    @GetMapping("/settings")
    public SettingsView settings() { return principal.settingsView(); }

    @PatchMapping("/settings")
    public SettingsView updateSettings(@RequestBody SettingsUpdate req) { return principal.updateSettings(req); }
}
