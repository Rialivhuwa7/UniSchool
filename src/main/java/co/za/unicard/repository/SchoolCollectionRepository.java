package co.za.unicard.repository;

import co.za.unicard.model.SchoolCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolCollectionRepository extends JpaRepository<SchoolCollection, Long> {
    List<SchoolCollection> findAllByOrderByCreatedAtDesc();
}
