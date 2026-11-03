package co.za.unicard.controller;

import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.HomeworkService;
import co.za.unicard.dto.HomeworkDtos.*;
import co.za.unicard.dto.NoticeDtos.EditRequest;
import co.za.unicard.dto.NoticeDtos.PostResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HomeworkController {

    private final HomeworkService service;
    private final CurrentUser currentUser;

    public HomeworkController(HomeworkService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/homework")
    @PreAuthorize("hasAnyRole('PARENT','LEARNER')")
    public List<HomeworkView> list(@RequestParam(required = false) Long learnerId) {
        return service.forMyLearners(currentUser.get(), learnerId);
    }

    @PutMapping("/api/homework/{id}/done")
    @PreAuthorize("hasAnyRole('PARENT','LEARNER')")
    public HomeworkView done(@PathVariable Long id, @Valid @RequestBody DoneRequest req) {
        return service.setDone(currentUser.get(), id, req);
    }

    @PostMapping("/api/teacher/homework")
    @PreAuthorize("hasRole('TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResult post(@Valid @RequestBody NewHomeworkRequest req) {
        return service.post(currentUser.get(), req);
    }

    @GetMapping("/api/teacher/homework")
    @PreAuthorize("hasRole('TEACHER')")
    public List<HomeworkStats> mine() {
        return service.mine(currentUser.get());
    }

    @PutMapping("/api/teacher/homework/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public PostResult resubmit(@PathVariable Long id, @Valid @RequestBody EditRequest req) {
        return service.resubmit(currentUser.get(), id, req);
    }
}
