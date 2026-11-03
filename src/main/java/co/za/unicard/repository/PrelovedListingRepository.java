package co.za.unicard.repository;

import co.za.unicard.model.Campus;
import co.za.unicard.model.PrelovedListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrelovedListingRepository extends JpaRepository<PrelovedListing, Long> {
    List<PrelovedListing> findByCampusAndSoldFalseOrderByListedAtDesc(Campus campus);
}
