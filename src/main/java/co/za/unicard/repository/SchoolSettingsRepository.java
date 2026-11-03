package co.za.unicard.repository;

import co.za.unicard.model.SchoolSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolSettingsRepository extends JpaRepository<SchoolSettings, Long> {
    default SchoolSettings current() {
        return findById(SchoolSettings.ID).orElseGet(() -> save(new SchoolSettings()));
    }
}
