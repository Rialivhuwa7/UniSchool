package co.za.unicard.repository;

import co.za.unicard.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    Optional<GroupMembership> findByGroupIdAndAccountId(Long groupId, Long accountId);
}
