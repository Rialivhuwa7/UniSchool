package co.za.unicard.service;

import co.za.unicard.exception.ForbiddenException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.Homework;
import co.za.unicard.model.HomeworkCompletion;
import co.za.unicard.model.Learner;
import co.za.unicard.model.PublishStatus;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.repository.HomeworkCompletionRepository;
import co.za.unicard.repository.HomeworkRepository;
import co.za.unicard.repository.LearnerRepository;
import co.za.unicard.repository.SchoolSettingsRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.util.Money;
import co.za.unicard.dto.HomeworkDtos.*;
import co.za.unicard.dto.NoticeDtos.EditRequest;
import co.za.unicard.dto.NoticeDtos.PostResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class HomeworkService {

    private final HomeworkRepository homework;
    private final HomeworkCompletionRepository completions;
    private final LearnerRepository learners;
    private final SchoolSettingsRepository settings;
    private final LearnerAccess access;
    private final WalletService wallets;
    private final SchoolCalendar calendar;

    public HomeworkService(HomeworkRepository homework, HomeworkCompletionRepository completions, LearnerRepository learners,
                           SchoolSettingsRepository settings, LearnerAccess access, WalletService wallets, SchoolCalendar calendar) {
        this.homework = homework;
        this.completions = completions;
        this.learners = learners;
        this.settings = settings;
        this.access = access;
        this.wallets = wallets;
        this.calendar = calendar;
    }

    /** Homework for each of the actor's learners (parent) or for themselves (learner). */
    @Transactional(readOnly = true)
    public List<HomeworkView> forMyLearners(Actor actor, Long learnerId) {
        List<HomeworkView> out = new ArrayList<>();
        for (Learner l : access.myLearners(actor)) {
            if (learnerId != null && !learnerId.equals(l.getId())) continue;
            for (Homework h : homework.findByCampusAndGradeAndStatusOrderByDueDateAsc(l.getCampus(), l.getGrade(), PublishStatus.PUBLISHED)) {
                out.add(new HomeworkView(h.getId(), l.getId(), l.getFirstName(), h.getSubject(), h.getTitle(), h.getBody(),
                        h.getDueDate(), h.getAttachmentName(), h.getByLabel(),
                        completions.existsByHomeworkIdAndLearnerId(h.getId(), l.getId())));
            }
        }
        return out;
    }

    public HomeworkView setDone(Actor actor, Long homeworkId, DoneRequest req) {
        Learner l = access.require(actor, req.learnerId());
        Homework h = homework.findById(homeworkId)
                .filter(x -> x.getStatus() == PublishStatus.PUBLISHED && x.getCampus() == l.getCampus() && x.getGrade().equals(l.getGrade()))
                .orElseThrow(() -> new NotFoundException("Homework", homeworkId));
        var existing = completions.findByHomeworkIdAndLearnerId(h.getId(), l.getId());
        if (req.done() && existing.isEmpty()) completions.save(new HomeworkCompletion(h, l, calendar.now()));
        if (!req.done()) existing.ifPresent(completions::delete);
        return new HomeworkView(h.getId(), l.getId(), l.getFirstName(), h.getSubject(), h.getTitle(), h.getBody(),
                h.getDueDate(), h.getAttachmentName(), h.getByLabel(), req.done());
    }

    // ---- teachers ----
    public PostResult post(Actor actor, NewHomeworkRequest req) {
        RoleAssignment t = teacher(actor);
        PublishStatus status = settings.current().isApproveHomework() ? PublishStatus.PENDING : PublishStatus.PUBLISHED;
        String body = req.body() == null || req.body().isBlank() ? "See your teacher's instructions in class." : req.body().trim();
        Homework h = homework.save(new Homework(t.getCampus(), t.getGrade(), req.subject(), req.title().trim(), body,
                req.dueDate(), req.attachmentName(), wallets.managed(actor.accountId()), actor.account().getDisplayName(),
                status, calendar.now()));
        return new PostResult(h.getId(), status, status == PublishStatus.PENDING
                ? "Sent to the principal for approval." : "Homework posted to " + t.getGrade() + ".");
    }

    @Transactional(readOnly = true)
    public List<HomeworkStats> mine(Actor actor) {
        return homework.findByAuthorIdOrderByCreatedAtDesc(actor.accountId()).stream().map(this::stats).toList();
    }

    public PostResult resubmit(Actor actor, Long id, EditRequest req) {
        Homework h = homework.findById(id).orElseThrow(() -> new NotFoundException("Homework", id));
        if (h.getAuthor() == null || !h.getAuthor().getId().equals(actor.accountId())) {
            throw new ForbiddenException("You can only edit your own homework.");
        }
        if (h.getStatus() == PublishStatus.PUBLISHED) throw new IllegalArgumentException("This homework is already published.");
        h.edit(req.title().trim(), req.body().trim());
        h.resubmit();
        return new PostResult(h.getId(), h.getStatus(), "Resubmitted to the principal.");
    }

    // ---- principal ----
    @Transactional(readOnly = true)
    public List<HomeworkStats> published() {
        return homework.findByStatusOrderByDueDateAsc(PublishStatus.PUBLISHED).stream().map(this::stats).toList();
    }

    @Transactional(readOnly = true)
    public List<HomeworkStats> pending() {
        return homework.findByStatusOrderByCreatedAtAsc(PublishStatus.PENDING).stream().map(this::stats).toList();
    }

    public PostResult approve(Long id) {
        Homework h = pendingHomework(id);
        h.publish();
        return new PostResult(h.getId(), h.getStatus(), "Approved. " + h.getGrade() + " learners and parents can see it.");
    }

    public PostResult requestChanges(Long id, String comment) {
        Homework h = pendingHomework(id);
        h.requestChanges(comment.trim());
        return new PostResult(h.getId(), h.getStatus(), "Sent back to the teacher with your comments.");
    }

    public PostResult editAndApprove(Long id, EditRequest req) {
        Homework h = pendingHomework(id);
        h.edit(req.title().trim(), req.body().trim());
        h.publish();
        return new PostResult(h.getId(), h.getStatus(), "Edited, approved and published.");
    }

    HomeworkStats stats(Homework h) {
        long total = learners.findByCampusAndGrade(h.getCampus(), h.getGrade()).size();
        long done = completions.countByHomeworkId(h.getId());
        return new HomeworkStats(h.getId(), h.getGrade(), h.getCampus().displayName(), h.getSubject(), h.getTitle(), h.getBody(),
                h.getDueDate(), h.getAttachmentName(), h.getByLabel(), h.getStatus(), h.getReviewComment(), h.getCreatedAt(),
                done, total, Money.pct(done, total));
    }

    private Homework pendingHomework(Long id) {
        Homework h = homework.findById(id).orElseThrow(() -> new NotFoundException("Homework", id));
        if (h.getStatus() != PublishStatus.PENDING) throw new IllegalArgumentException("This homework isn't waiting for approval.");
        return h;
    }

    static RoleAssignment teacher(Actor actor) {
        RoleAssignment t = actor.roleAssignment();
        if (t.getGrade() == null || t.getCampus() == null) throw new ForbiddenException("Your teacher profile has no class yet.");
        return t;
    }
}
