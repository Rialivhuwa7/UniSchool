package co.za.unicard.repository;

import co.za.unicard.model.Campus;
import co.za.unicard.model.SupplyList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplyListRepository extends JpaRepository<SupplyList, Long> {
    Optional<SupplyList> findByCampusAndGrade(Campus campus, String grade);
    List<SupplyList> findAllByOrderByCampusAscGradeAsc();
}
