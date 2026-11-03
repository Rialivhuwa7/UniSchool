package co.za.unicard.repository;

import co.za.unicard.model.HomeworkCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HomeworkCompletionRepository extends JpaRepository<HomeworkCompletion, Long> {
    Optional<HomeworkCompletion> findByHomeworkIdAndLearnerId(Long homeworkId, Long learnerId);
    boolean existsByHomeworkIdAndLearnerId(Long homeworkId, Long learnerId);
    long countByHomeworkId(Long homeworkId);
}
