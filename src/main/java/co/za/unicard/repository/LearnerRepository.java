package co.za.unicard.repository;

import co.za.unicard.model.Campus;
import co.za.unicard.model.Learner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LearnerRepository extends JpaRepository<Learner, Long> {

    @Query("select l from Learner l join l.guardians g where g.id = :accountId order by l.id")
    List<Learner> findByGuardian(@Param("accountId") Long accountId);

    Optional<Learner> findByCardToken(String cardToken);

    Optional<Learner> findByStudentNumber(String studentNumber);

    List<Learner> findByCampusAndGrade(Campus campus, String grade);

    List<Learner> findByCampus(Campus campus);

    @Query("select l from Learner l where lower(l.firstName) like lower(concat('%', :q, '%')) " +
           "or lower(l.lastName) like lower(concat('%', :q, '%')) or l.studentNumber like concat('%', :q, '%') order by l.firstName")
    List<Learner> search(@Param("q") String q);
}
