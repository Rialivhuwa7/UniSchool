package co.za.unicard.repository;

import co.za.unicard.model.NoticeReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoticeReceiptRepository extends JpaRepository<NoticeReceipt, Long> {

    boolean existsByNoticeIdAndAccountIdAndType(Long noticeId, Long accountId, NoticeReceipt.Type type);

    boolean existsByNoticeIdAndLearnerIdAndType(Long noticeId, Long learnerId, NoticeReceipt.Type type);

    Optional<NoticeReceipt> findByNoticeIdAndAccountIdAndType(Long noticeId, Long accountId, NoticeReceipt.Type type);

    long countByNoticeIdAndType(Long noticeId, NoticeReceipt.Type type);

    @Query("select r.learner.id from NoticeReceipt r where r.notice.id = :noticeId and r.type = co.za.unicard.model.NoticeReceipt.Type.ACK")
    List<Long> ackedLearnerIds(@Param("noticeId") Long noticeId);
}
