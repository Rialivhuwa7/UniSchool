package co.za.unicard.repository;

import co.za.unicard.model.AccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    List<AccessRequest> findByStatusOrderByCreatedAtAsc(AccessRequest.Status status);
}
