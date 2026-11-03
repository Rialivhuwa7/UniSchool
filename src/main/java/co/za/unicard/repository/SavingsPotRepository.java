package co.za.unicard.repository;

import co.za.unicard.model.SavingsPot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavingsPotRepository extends JpaRepository<SavingsPot, Long> {
    Optional<SavingsPot> findByAccountId(Long accountId);
    Optional<SavingsPot> findByShareToken(String shareToken);
}
