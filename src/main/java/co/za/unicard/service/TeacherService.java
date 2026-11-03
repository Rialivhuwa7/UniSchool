package co.za.unicard.service;

import co.za.unicard.exception.ForbiddenException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.Homework;
import co.za.unicard.model.Learner;
import co.za.unicard.model.Notice;
import co.za.unicard.model.NoticeReceipt;
import co.za.unicard.model.PublishStatus;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.repository.HomeworkCompletionRepository;
import co.za.unicard.repository.HomeworkRepository;
import co.za.unicard.repository.LearnerRepository;
import co.za.unicard.repository.NoticeReceiptRepository;
import co.za.unicard.repository.NoticeRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.util.Audience;
import co.za.unicard.dto.SchoolDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TeacherService {

    private final LearnerRepository learners;
    private final HomeworkRepository homework;
    private final HomeworkCompletionRepository completions;
    private final NoticeRepository notices;
    private final NoticeReceiptRepository receipts;

    public TeacherService(LearnerRepository learners, HomeworkRepository homework, HomeworkCompletionRepository completions,
                          NoticeRepository notices, NoticeReceiptRepository receipts) {
        this.learners = learners;
        this.homework = homework;
        this.completions = completions;
        this.notices = notices;
        this.receipts = receipts;
    }

    /** The teacher's class: per learner, homework done and notices acknowledged. */
    public ClassView myClass(Actor actor) {
        RoleAssignment t = teacher(actor);
        List<Learner> kids = learners.findByCampusAndGrade(t.getCampus(), t.getGrade());
        List<Homework> hw = homework.findByCampusAndGradeAndStatusOrderByDueDateAsc(t.getCampus(), t.getGrade(), PublishStatus.PUBLISHED);
        List<Notice> ackNotices = notices.findByStatusOrderByPublishedAtDesc(PublishStatus.PUBLISHED).stream()
                .filter(Notice::isRequiresAck).toList();
        List<RosterRow> rows = kids.stream().map(k -> {
            long done = hw.stream().filter(h -> completions.existsByHomeworkIdAndLearnerId(h.getId(), k.getId())).count();
            List<Notice> forK = ackNotices.stream().filter(n -> Audience.matches(k, n.getCampus(), n.getGrades(), n.getTeams())).toList();
            long acked = forK.stream().filter(n -> receipts.existsByNoticeIdAndLearnerIdAndType(n.getId(), k.getId(), NoticeReceipt.Type.ACK)).count();
            return new RosterRow(k.getId(), k.getShortName(), done, hw.size(), acked, forK.size());
        }).toList();
        return new ClassView(actor.account().getDisplayName(), t.getGrade(), t.getClassName(), t.getCampus().displayName(), rows);
    }

    /** Nudge one learner's parent. Delivery (push/SMS) isn't built yet; this confirms who would be told. */
    public Message nudge(Actor actor, Long learnerId) {
        RoleAssignment t = teacher(actor);
        Learner l = learners.findById(learnerId).orElseThrow(() -> new NotFoundException("Learner", learnerId));
        if (l.getCampus() != t.getCampus() || !l.getGrade().equals(t.getGrade())) {
            throw new ForbiddenException("You can only nudge parents in your class.");
        }
        return new Message("Reminder sent to " + l.getShortName() + "'s parent.");
    }

    private static RoleAssignment teacher(Actor actor) {
        RoleAssignment t = actor.roleAssignment();
        if (t.getGrade() == null || t.getCampus() == null) throw new ForbiddenException("Your teacher profile has no class yet.");
        return t;
    }
}
