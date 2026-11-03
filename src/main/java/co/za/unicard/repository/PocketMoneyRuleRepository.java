package co.za.unicard.repository;

import co.za.unicard.model.PocketMoneyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;

public interface PocketMoneyRuleRepository extends JpaRepository<PocketMoneyRule, Long> {

    @Query("select r from PocketMoneyRule r join fetch r.learner where r.account.id = :accountId order by r.id")
    List<PocketMoneyRule> findByAccount(@Param("accountId") Long accountId);

    List<PocketMoneyRule> findByEnabledTrueAndDayOfWeek(DayOfWeek dayOfWeek);
}
