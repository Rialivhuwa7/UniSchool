package co.za.unicard.repository;

import co.za.unicard.model.Campus;
import co.za.unicard.model.UniformItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UniformItemRepository extends JpaRepository<UniformItem, Long> {
    List<UniformItem> findByCampusOrderByIdAsc(Campus campus);
}
