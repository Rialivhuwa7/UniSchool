package co.za.unicard.service;

import co.za.unicard.dto.SetupDtos.LearnerSignIn;
import co.za.unicard.dto.SetupDtos.LearnerSignInRequest;
import co.za.unicard.exception.ConflictException;
import co.za.unicard.model.Learner;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.UserAccountRepository;
import co.za.unicard.security.Actor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Learners don't sign up themselves: a parent makes their child's sign-in. The account only
 * holds the LEARNER role for that one child and is verified straight away, because the parent
 * is already verified as the child's guardian.
 */
@Service
@Transactional
public class LearnerSignInService {

    private final LearnerAccess access;
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public LearnerSignInService(LearnerAccess access, UserAccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.access = access;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LearnerSignIn get(Actor actor, Long learnerId) {
        Learner l = access.requireAsParent(actor, learnerId);
        return new LearnerSignIn(accounts.findLearnerSignIn(l.getId()).map(r -> r.getAccount().getUsername()).orElse(null));
    }

    public LearnerSignIn create(Actor actor, Long learnerId, LearnerSignInRequest req) {
        Learner l = access.requireAsParent(actor, learnerId);
        if (accounts.findLearnerSignIn(l.getId()).isPresent()) {
            throw new ConflictException(l.getFirstName() + " already has a sign-in.");
        }
        String username = req.username().trim();
        if (accounts.existsByUsername(username)) throw new ConflictException("That username is taken.");
        UserAccount account = new UserAccount(username, passwordEncoder.encode(req.password()),
                l.getFullName(), l.getFirstName(), "Learner");
        account.addRole(RoleAssignment.learner(l));
        accounts.save(account);
        return new LearnerSignIn(username);
    }
}
