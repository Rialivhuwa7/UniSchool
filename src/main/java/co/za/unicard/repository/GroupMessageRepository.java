package co.za.unicard.repository;

import co.za.unicard.model.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    List<GroupMessage> findByGroupIdAndHiddenFalseOrderBySentAtAscIdAsc(Long groupId);
    List<GroupMessage> findByReportsGreaterThanAndHiddenFalseOrderBySentAtDesc(int reports);
    long countByReportsGreaterThanAndHiddenFalse(int reports);
}
