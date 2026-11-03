package co.za.unicard.controller;

import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.GroupService;
import co.za.unicard.dto.GroupDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@PreAuthorize("hasAnyRole('PARENT','TEACHER')")
public class GroupController {

    private final GroupService service;
    private final CurrentUser currentUser;

    public GroupController(GroupService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<GroupView> groups() {
        return service.myGroups(currentUser.get());
    }

    @GetMapping("/{id}/messages")
    public List<MessageView> messages(@PathVariable Long id) {
        return service.messages(currentUser.get(), id);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public PostMessageResult post(@PathVariable Long id, @Valid @RequestBody PostMessageRequest req) {
        return service.post(currentUser.get(), id, req.body());
    }

    @PostMapping("/messages/{messageId}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@PathVariable Long messageId) {
        service.report(currentUser.get(), messageId);
    }

    @PutMapping("/{id}/mute")
    public GroupView mute(@PathVariable Long id, @RequestBody MuteRequest req) {
        return service.mute(currentUser.get(), id, req.muted());
    }
}
