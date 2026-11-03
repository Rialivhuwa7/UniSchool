package co.za.unicard.controller;

import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.NoticeService;
import co.za.unicard.dto.NoticeDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class NoticeController {

    private final NoticeService service;
    private final CurrentUser currentUser;

    public NoticeController(NoticeService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    // ---- parents ----
    @GetMapping("/api/notices")
    @PreAuthorize("hasRole('PARENT')")
    public List<NoticeView> notices(@RequestParam(required = false) Long learnerId) {
        return service.forParent(currentUser.get(), learnerId);
    }

    @PostMapping("/api/notices/{id}/read")
    @PreAuthorize("hasRole('PARENT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable Long id) {
        service.markRead(currentUser.get(), id);
    }

    @PostMapping("/api/notices/read-all")
    @PreAuthorize("hasRole('PARENT')")
    public Map<String, Integer> readAll() {
        return Map.of("marked", service.markAllRead(currentUser.get()));
    }

    @PostMapping("/api/notices/{id}/acknowledge")
    @PreAuthorize("hasRole('PARENT')")
    public NoticeView acknowledge(@PathVariable Long id, @Valid @RequestBody AckRequest req) {
        return service.acknowledge(currentUser.get(), id, req.learnerId());
    }

    @PostMapping("/api/notices/{id}/rsvp")
    @PreAuthorize("hasRole('PARENT')")
    public NoticeView rsvp(@PathVariable Long id, @Valid @RequestBody RsvpRequest req) {
        return service.rsvp(currentUser.get(), id, req.attending());
    }

    // ---- learners ----
    @GetMapping("/api/me/headlines")
    @PreAuthorize("hasRole('LEARNER')")
    public List<Headline> headlines() {
        return service.headlines(currentUser.get());
    }

    // ---- staff ----
    @PostMapping("/api/staff/notices")
    @PreAuthorize("hasAnyRole('TEACHER','PRINCIPAL','SCHOOL_OFFICE')")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResult post(@Valid @RequestBody NewNoticeRequest req) {
        return service.post(currentUser.get(), req);
    }

    @GetMapping("/api/staff/notices/mine")
    @PreAuthorize("hasAnyRole('TEACHER','PRINCIPAL','SCHOOL_OFFICE')")
    public List<NoticeStats> mine() {
        return service.mine(currentUser.get());
    }

    @GetMapping("/api/staff/notices")
    @PreAuthorize("hasAnyRole('PRINCIPAL','SCHOOL_OFFICE')")
    public List<NoticeStats> all() {
        return service.allPublished();
    }

    @PutMapping("/api/staff/notices/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public PostResult resubmit(@PathVariable Long id, @Valid @RequestBody EditRequest req) {
        return service.resubmit(currentUser.get(), id, req);
    }
}
