package co.za.unicard.controller;

import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.ParentHomeService;
import co.za.unicard.dto.SchoolDtos.ParentHome;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParentHomeController {

    private final ParentHomeService home;
    private final CurrentUser currentUser;

    public ParentHomeController(ParentHomeService home, CurrentUser currentUser) {
        this.home = home;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/home")
    @PreAuthorize("hasRole('PARENT')")
    public ParentHome home() { return home.home(currentUser.get()); }
}
