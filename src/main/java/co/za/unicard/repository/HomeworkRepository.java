package co.za.unicard.repository;

import co.za.unicard.model.Campus;
import co.za.unicard.model.Homework;
import co.za.unicard.model.PublishStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {
    List<Homework> findByCampusAndGradeAndStatusOrderByDueDateAsc(Campus campus, String grade, PublishStatus status);
    List<Homework> findByStatusOrderByDueDateAsc(PublishStatus status);
    List<Homework> findByStatusOrderByCreatedAtAsc(PublishStatus status);
    List<Homework> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
}
