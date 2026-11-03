package co.za.unicard.service;

import co.za.unicard.model.Campus;
import co.za.unicard.model.Learner;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.LearnerRepository;
import co.za.unicard.util.Audience;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Works out who a notice, collection or homework is for. Loads learners and filters in memory,
 * which is fine for a school of a few thousand; move to a query if it grows.
 */
@Service
@Transactional(readOnly = true)
public class AudienceService {

    private final LearnerRepository learners;

    public AudienceService(LearnerRepository learners) { this.learners = learners; }

    public List<Learner> learners(Campus campus, Collection<String> grades, Collection<String> teams) {
        List<Learner> pool = campus == Campus.BOTH || campus == null ? learners.findAll() : learners.findByCampus(campus);
        return pool.stream().filter(l -> Audience.matches(l, campus, grades, teams)).toList();
    }

    /** Distinct guardian accounts of the matching learners. */
    public Set<UserAccount> guardians(Campus campus, Collection<String> grades, Collection<String> teams) {
        return learners(campus, grades, teams).stream().flatMap(l -> l.getGuardians().stream()).collect(Collectors.toSet());
    }

    public long guardianCount(Campus campus, Collection<String> grades, Collection<String> teams) {
        return guardians(campus, grades, teams).size();
    }
}
