package co.za.unicard.repository;

import co.za.unicard.model.PreOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PreOrderRepository extends JpaRepository<PreOrder, Long> {

    Optional<PreOrder> findByLearnerIdAndDay(Long learnerId, LocalDate day);

    @Query("select o from PreOrder o where o.learner.id in :learnerIds and o.day between :from and :to order by o.day")
    List<PreOrder> findForLearners(@Param("learnerIds") Collection<Long> learnerIds,
                                   @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select o from PreOrder o join fetch o.learner where o.day = :day order by o.learner.firstName")
    List<PreOrder> findByDayWithLearner(@Param("day") LocalDate day);

    List<PreOrder> findByDayAndRepeatWeeklyTrue(LocalDate day);
}
