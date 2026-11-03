package co.za.unicard.controller;

import co.za.unicard.security.CurrentUser;
import co.za.unicard.service.AuthService;
import co.za.unicard.dto.AuthDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final CurrentUser currentUser;

    public AuthController(AuthService auth, CurrentUser currentUser) {
        this.auth = auth;
        this.currentUser = currentUser;
    }

    /** Sign in with username and password. Returns a JWT for the first verified role. */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    /** Sign up. The account can sign in once the school office approves the request. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @GetMapping("/me")
    public MeResponse me() {
        return auth.me(currentUser.get());
    }

    /** Switch to another verified role on the same account (e.g. teacher to parent). Returns a new JWT. */
    @PostMapping("/switch-role")
    public TokenResponse switchRole(@Valid @RequestBody SwitchRoleRequest req) {
        return auth.switchRole(currentUser.get(), req.role());
    }
}
