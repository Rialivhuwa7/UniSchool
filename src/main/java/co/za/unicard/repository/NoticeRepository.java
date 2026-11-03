package co.za.unicard.repository;

import co.za.unicard.model.Notice;
import co.za.unicard.model.PublishStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByStatusOrderByPublishedAtDesc(PublishStatus status);
    List<Notice> findByStatusOrderByCreatedAtAsc(PublishStatus status);
    List<Notice> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
}
