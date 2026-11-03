package co.za.unicard.repository;

import co.za.unicard.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("select c from CartItem c join fetch c.learner where c.account.id = :accountId order by c.id")
    List<CartItem> findByAccount(@Param("accountId") Long accountId);
}
