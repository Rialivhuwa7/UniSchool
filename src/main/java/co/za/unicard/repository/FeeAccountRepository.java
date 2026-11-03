package co.za.unicard.repository;

import co.za.unicard.model.FeeAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeeAccountRepository extends JpaRepository<FeeAccount, Long> {

    Optional<FeeAccount> findByLearnerIdAndFeeYear(Long learnerId, int feeYear);

    List<FeeAccount> findByFeeYear(int feeYear);

    /** [plan (null = none), count] */
    @Query("select f.plan, count(f) from FeeAccount f where f.feeYear = :year group by f.plan")
    List<Object[]> planCounts(@Param("year") int year);
}
