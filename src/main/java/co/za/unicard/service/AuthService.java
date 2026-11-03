package co.za.unicard.service;

import co.za.unicard.exception.ConflictException;
import co.za.unicard.exception.ForbiddenException;
import co.za.unicard.model.AccessRequest;
import co.za.unicard.model.Campus;
import co.za.unicard.model.Learner;
import co.za.unicard.model.RequestedLearner;
import co.za.unicard.model.Role;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.AccessRequestRepository;
import co.za.unicard.repository.LearnerRepository;
import co.za.unicard.repository.UserAccountRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.security.JwtService;
import co.za.unicard.dto.AuthDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class AuthService {

    private final UserAccountRepository accounts;
    private final AccessRequestRepository requests;
    private final LearnerRepository learners;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final SchoolCalendar calendar;
    private final SchoolOfficeService office;
    private final boolean autoApprove;

    public AuthService(UserAccountRepository accounts, AccessRequestRepository requests, LearnerRepository learners,
                       PasswordEncoder passwordEncoder, JwtService jwt, SchoolCalendar calendar, SchoolOfficeService office,
                       @Value("${unischool.auto-approve-signups:false}") boolean autoApprove) {
        this.accounts = accounts;
        this.requests = requests;
        this.learners = learners;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.calendar = calendar;
        this.office = office;
        this.autoApprove = autoApprove;
    }

    public TokenResponse login(LoginRequest req) {
        UserAccount account = accounts.findByUsername(req.username().trim())
                .filter(a -> passwordEncoder.matches(req.password(), a.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("That username and password don't match."));
        if (!account.isActive()) throw new ForbiddenException("This account has been deactivated.");
        RoleAssignment role = account.firstVerifiedRole()
                .orElseThrow(() -> new ForbiddenException("Your request is waiting for the school office to approve it. You'll be able to sign in once they do. If it's taking long, contact the school office."));
        return token(account, role);
    }

    public TokenResponse switchRole(Actor actor, Role target) {
        RoleAssignment role = actor.account().getRoles().stream()
                .filter(r -> r.getRole() == target && r.isVerified())
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("You don't have a verified " + target + " role."));
        return token(actor.account(), role);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Actor actor) {
        return MeResponse.of(actor.account(), actor.roleAssignment());
    }

    /**
     * Anyone can ask for access with any role; nobody can sign in until the school office
     * approves. Learners named here who are already in UniSchool (matched by student number)
     * are linked on approval; the others are created on approval from these details.
     */
    public RegisterResponse register(RegisterRequest req) {
        String username = req.username().trim();
        if (accounts.existsByUsername(username)) throw new ConflictException("That username is taken.");

        List<LearnerDetails> named = switch (req.role()) {
            case PARENT -> req.children() == null ? List.of() : req.children();
            case LEARNER -> req.learner() == null ? List.of() : List.of(req.learner());
            default -> List.of();
        };
        if (req.role() == Role.LEARNER && named.isEmpty()) {
            throw new IllegalArgumentException("Tell us your grade and school.");
        }
        Set<Long> known = new LinkedHashSet<>();
        List<RequestedLearner> newOnes = new ArrayList<>();
        for (LearnerDetails d : named) {
            if (d.campus() == Campus.BOTH) throw new IllegalArgumentException("Choose Prep or College for " + d.firstName() + ".");
            String number = d.studentNumber() == null || d.studentNumber().isBlank() ? null : d.studentNumber().trim();
            Optional<Learner> existing = number == null ? Optional.empty() : learners.findByStudentNumber(number);
            if (existing.isPresent()) known.add(existing.get().getId());
            else newOnes.add(new RequestedLearner(number, d.firstName().trim(), d.lastName().trim(), d.grade().trim(), d.campus()));
        }

        UserAccount account = new UserAccount(username, passwordEncoder.encode(req.password()),
                req.fullName().trim(), req.fullName().trim(), req.role().isStaff() ? "Staff" : req.role() == Role.LEARNER ? "Learner" : "Parent");
        String label = req.ask() != null && !req.ask().isBlank() ? req.ask().trim() : askFor(req.role(), named);
        account.addRole(new RoleAssignment(req.role(), label, req.role().isStaff() ? Campus.BOTH : null, false));
        accounts.save(account);

        String how = named.isEmpty() ? "Signed up in the app"
                : known.size() + " learner" + (known.size() == 1 ? "" : "s") + " already in UniSchool, "
                  + newOnes.size() + " new";
        AccessRequest ar = new AccessRequest(account, req.role(), label, how, known, calendar.now());
        ar.getNewLearners().addAll(newOnes);
        requests.save(ar);
        if (autoApprove) {
            // Local testing only (unischool.auto-approve-signups): approve as the office would, straight away.
            office.approveAccess(ar.getId(), req.teaching());
            return new RegisterResponse(ar.getId(), "Your account is ready. You can sign in now.");
        }
        return new RegisterResponse(ar.getId(), "Thanks. The school will check your request and you'll be able to sign in once they approve it.");
    }

    private static String askFor(Role role, List<LearnerDetails> named) {
        if (named.isEmpty()) return role.name().charAt(0) + role.name().substring(1).toLowerCase().replace('_', ' ');
        String who = String.join(", ", named.stream().map(d -> d.firstName().trim() + " (" + d.grade().trim() + ")").toList());
        return role == Role.LEARNER ? "Learner, " + who : "Parent of " + who;
    }

    private TokenResponse token(UserAccount account, RoleAssignment role) {
        JwtService.IssuedToken t = jwt.issue(account, role);
        return new TokenResponse(t.token(), t.expiresAt(), MeResponse.of(account, role));
    }
}
