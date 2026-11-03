package co.za.unicard.repository;

import co.za.unicard.model.RoleAssignment;
import co.za.unicard.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("select r from RoleAssignment r join fetch r.account a where r.verified = true and r.role <> co.za.unicard.model.Role.PARENT and r.role <> co.za.unicard.model.Role.LEARNER order by a.fullName")
    List<RoleAssignment> findVerifiedStaffRoles();

    @Query("select r.learner.id from RoleAssignment r where r.learner is not null")
    List<Long> learnerIdsWithSignIn();

    @Query("select r from RoleAssignment r join fetch r.account where r.learner.id = :learnerId")
    Optional<RoleAssignment> findLearnerSignIn(@Param("learnerId") Long learnerId);

    /** Accounts that can run the school: a verified principal or school office role. */
    @Query("select count(r) > 0 from RoleAssignment r where r.verified = true and (r.role = co.za.unicard.model.Role.PRINCIPAL or r.role = co.za.unicard.model.Role.SCHOOL_OFFICE)")
    boolean hasSchoolAdmin();
}
