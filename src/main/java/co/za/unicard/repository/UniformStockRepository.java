package co.za.unicard.repository;

import co.za.unicard.model.UniformStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UniformStockRepository extends JpaRepository<UniformStock, Long> {
    List<UniformStock> findAllByOrderByItemNameAscSizeAsc();
}
