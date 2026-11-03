package co.za.unicard.repository;

import co.za.unicard.model.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
    List<ShopOrder> findByAccountIdOrderByPlacedAtDesc(Long accountId);

    /** [grade, campus, number of learners with a supply-list order] */
    @Query("select l.learner.grade, l.learner.campus, count(distinct l.learner.id) from ShopOrderLine l " +
           "where l.category = co.za.unicard.model.ProductCategory.SUPPLY_LIST group by l.learner.grade, l.learner.campus")
    List<Object[]> supplyListOrdersByGrade();

    @Query("select coalesce(sum(l.unitPrice * l.quantity), 0) from ShopOrderLine l where l.category = co.za.unicard.model.ProductCategory.UNIFORM")
    java.math.BigDecimal uniformSales();
}
