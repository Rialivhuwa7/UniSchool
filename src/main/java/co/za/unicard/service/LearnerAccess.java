package co.za.unicard.service;

import co.za.unicard.exception.ForbiddenException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.Learner;
import co.za.unicard.model.Role;
import co.za.unicard.repository.LearnerRepository;
import co.za.unicard.security.Actor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Who may see or act for a learner: their guardians (as PARENT) and the learner themselves. */
@Component
public class LearnerAccess {

    private final LearnerRepository learners;

    public LearnerAccess(LearnerRepository learners) { this.learners = learners; }

    /** The learners this actor looks after: a parent's children, or the learner themselves. */
    public List<Learner> myLearners(Actor actor) {
        if (actor.is(Role.PARENT)) return learners.findByGuardian(actor.accountId());
        if (actor.is(Role.LEARNER)) return List.of(self(actor));
        return List.of();
    }

    public Learner self(Actor actor) {
        if (!actor.is(Role.LEARNER) || actor.roleAssignment().getLearner() == null) {
            throw new ForbiddenException("Switch to your learner role first.");
        }
        Long id = actor.roleAssignment().getLearner().getId();
        return learners.findById(id).orElseThrow(() -> new NotFoundException("Learner", id));
    }

    /** Loads the learner and checks the actor is their parent (or is that learner). */
    public Learner require(Actor actor, Long learnerId) {
        Learner l = learners.findById(learnerId).orElseThrow(() -> new NotFoundException("Learner", learnerId));
        boolean ok = (actor.is(Role.PARENT) && l.isGuardian(actor.accountId()))
                || (actor.is(Role.LEARNER) && actor.roleAssignment().getLearner() != null
                    && actor.roleAssignment().getLearner().getId().equals(learnerId));
        if (!ok) throw new ForbiddenException("You can only do this for your own children.");
        return l;
    }

    /** Parent-only actions such as changing card controls. */
    public Learner requireAsParent(Actor actor, Long learnerId) {
        if (!actor.is(Role.PARENT)) throw new ForbiddenException("Only a parent can change this.");
        return require(actor, learnerId);
    }
}
