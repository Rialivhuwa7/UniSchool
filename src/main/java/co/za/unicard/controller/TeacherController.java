package co.za.unicard.controller;

import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.TeacherService;
import co.za.unicard.dto.SchoolDtos.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    private final TeacherService teacher;
    private final CurrentUser currentUser;

    public TeacherController(TeacherService teacher, CurrentUser currentUser) {
        this.teacher = teacher;
        this.currentUser = currentUser;
    }

    @GetMapping("/class")
    public ClassView myClass() { return teacher.myClass(currentUser.get()); }

    @PostMapping("/class/{learnerId}/nudge")
    public Message nudge(@PathVariable Long learnerId) { return teacher.nudge(currentUser.get(), learnerId); }
}
