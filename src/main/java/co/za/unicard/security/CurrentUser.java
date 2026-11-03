package co.za.unicard.security;

import co.za.unicard.exception.ForbiddenException;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Resolves the JWT on the current request into an {@link Actor}. */
@Component
public class CurrentUser {

    private final UserAccountRepository accounts;

    public CurrentUser(UserAccountRepository accounts) { this.accounts = accounts; }

    public Actor get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ForbiddenException("Sign in first.");
        }
        UserAccount account = accounts.findByUsername(jwt.getSubject())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ForbiddenException("This account is no longer active."));
        Object rid = jwt.getClaims().get("rid");
        Long roleId = rid instanceof Number n ? n.longValue() : null;
        RoleAssignment role = account.getRoles().stream()
                .filter(r -> r.getId().equals(roleId) && r.isVerified())
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("This role is no longer available. Sign in again."));
        return new Actor(account, role);
    }
}
