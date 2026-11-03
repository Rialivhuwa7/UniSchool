package co.za.unicard.repository;

import co.za.unicard.model.SportOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportOfferRepository extends JpaRepository<SportOffer, Long> {
    List<SportOffer> findBySportInOrderByIdAsc(java.util.Collection<String> sports);
}
