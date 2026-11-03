package co.za.unicard.repository;

import co.za.unicard.model.CollectionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CollectionPaymentRepository extends JpaRepository<CollectionPayment, Long> {

    List<CollectionPayment> findByCollectionIdAndLearnerId(Long collectionId, Long learnerId);

    @Query("select coalesce(sum(p.amount), 0) from CollectionPayment p where p.collection.id = :id")
    BigDecimal totalFor(@Param("id") Long collectionId);

    @Query("select count(distinct p.learner.id) from CollectionPayment p where p.collection.id = :id")
    long learnersPaid(@Param("id") Long collectionId);

    @Query("select coalesce(sum(p.amount), 0) from CollectionPayment p")
    BigDecimal totalCollected();
}
