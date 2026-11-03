package co.za.unicard.service;

import co.za.unicard.exception.ForbiddenException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.Campus;
import co.za.unicard.model.Learner;
import co.za.unicard.model.Notice;
import co.za.unicard.model.NoticeReceipt;
import co.za.unicard.model.PublishStatus;
import co.za.unicard.model.Role;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.model.SchoolCollection;
import co.za.unicard.model.UserAccount;
import co.za.unicard.repository.NoticeReceiptRepository;
import co.za.unicard.repository.NoticeRepository;
import co.za.unicard.repository.SchoolCollectionRepository;
import co.za.unicard.repository.SchoolSettingsRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.util.Audience;
import co.za.unicard.util.Money;
import co.za.unicard.dto.NoticeDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class NoticeService {

    private final NoticeRepository notices;
    private final NoticeReceiptRepository receipts;
    private final SchoolCollectionRepository collections;
    private final SchoolSettingsRepository settings;
    private final AudienceService audience;
    private final LearnerAccess access;
    private final WalletService wallets;
    private final SchoolCalendar calendar;

    public NoticeService(NoticeRepository notices, NoticeReceiptRepository receipts, SchoolCollectionRepository collections,
                         SchoolSettingsRepository settings, AudienceService audience, LearnerAccess access,
                         WalletService wallets, SchoolCalendar calendar) {
        this.notices = notices;
        this.receipts = receipts;
        this.collections = collections;
        this.settings = settings;
        this.audience = audience;
        this.access = access;
        this.wallets = wallets;
        this.calendar = calendar;
    }

    // ---------------- parents ----------------

    /** Published notices for the parent's children; optionally only one child. */
    @Transactional(readOnly = true)
    public List<NoticeView> forParent(Actor actor, Long learnerId) {
        List<Learner> kids = access.myLearners(actor);
        if (learnerId != null) kids = kids.stream().filter(k -> k.getId().equals(learnerId)).toList();
        List<NoticeView> out = new ArrayList<>();
        for (Notice n : notices.findByStatusOrderByPublishedAtDesc(PublishStatus.PUBLISHED)) {
            List<Learner> forKids = kids.stream().filter(k -> matches(n, k)).toList();
            if (forKids.isEmpty()) continue;
            out.add(view(n, actor.accountId(), forKids));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public long unreadCount(Actor actor) {
        return forParent(actor, null).stream().filter(v -> !v.read()).count();
    }

    public void markRead(Actor actor, Long noticeId) {
        Notice n = visibleTo(actor, noticeId);
        read(n, actor);
    }

    public int markAllRead(Actor actor) {
        int n = 0;
        for (NoticeView v : forParent(actor, null)) {
            if (!v.read()) {
                read(notices.getReferenceById(v.id()), actor);
                n++;
            }
        }
        return n;
    }

    public NoticeView acknowledge(Actor actor, Long noticeId, Long learnerId) {
        Notice n = visibleTo(actor, noticeId);
        Learner l = access.requireAsParent(actor, learnerId);
        if (!n.isRequiresAck()) throw new IllegalArgumentException("This notice doesn't need acknowledging.");
        if (!matches(n, l)) throw new IllegalArgumentException("This notice isn't for " + l.getFirstName() + ".");
        if (!receipts.existsByNoticeIdAndLearnerIdAndType(n.getId(), l.getId(), NoticeReceipt.Type.ACK)) {
            receipts.save(new NoticeReceipt(n, account(actor), l, NoticeReceipt.Type.ACK, null, calendar.now()));
        }
        read(n, actor);
        return view(n, actor.accountId(), access.myLearners(actor).stream().filter(k -> matches(n, k)).toList());
    }

    public NoticeView rsvp(Actor actor, Long noticeId, boolean attending) {
        Notice n = visibleTo(actor, noticeId);
        if (!n.isRsvpEnabled()) throw new IllegalArgumentException("This notice doesn't take RSVPs.");
        receipts.findByNoticeIdAndAccountIdAndType(n.getId(), actor.accountId(), NoticeReceipt.Type.RSVP)
                .ifPresentOrElse(r -> r.setAttending(attending),
                        () -> receipts.save(new NoticeReceipt(n, account(actor), null, NoticeReceipt.Type.RSVP, attending, calendar.now())));
        read(n, actor);
        return view(n, actor.accountId(), access.myLearners(actor).stream().filter(k -> matches(n, k)).toList());
    }

    // ---------------- learners ----------------

    @Transactional(readOnly = true)
    public List<Headline> headlines(Actor actor) {
        Learner me = access.self(actor);
        return notices.findByStatusOrderByPublishedAtDesc(PublishStatus.PUBLISHED).stream()
                .filter(n -> matches(n, me)).limit(5)
                .map(n -> new Headline(n.getId(), n.getFromLabel(), n.getTag(), n.getTitle(), n.getPublishedAt())).toList();
    }

    // ---------------- staff ----------------

    /**
     * Teachers post to their own campus; if the principal reviews teachers' notices, it goes in
     * as PENDING. The principal and school office publish directly.
     */
    public PostResult post(Actor actor, NewNoticeRequest req) {
        RoleAssignment role = actor.roleAssignment();
        boolean hasGrades = req.grades() != null && !req.grades().isEmpty();
        boolean hasTeams = req.teams() != null && !req.teams().isEmpty();
        if (!hasGrades && !hasTeams) throw new IllegalArgumentException("Choose a grade or a team.");
        String from;
        PublishStatus status = PublishStatus.PUBLISHED;
        switch (role.getRole()) {
            case TEACHER -> {
                if (role.getCampus() != null && role.getCampus() != Campus.BOTH && req.campus() != role.getCampus()) {
                    throw new ForbiddenException("Teachers can post to their own school only.");
                }
                from = actor.account().getDisplayName() + ", " + role.getLabel().split(",")[0];
                if (settings.current().isApproveNotices()) status = PublishStatus.PENDING;
            }
            case PRINCIPAL -> from = "Principal's office";
            case SCHOOL_OFFICE -> from = "School office";
            default -> throw new ForbiddenException("Only staff can post notices.");
        }
        SchoolCollection link = req.linkedCollectionId() == null ? null
                : collections.findById(req.linkedCollectionId()).orElseThrow(() -> new NotFoundException("Collection", req.linkedCollectionId()));
        Notice n = notices.save(new Notice(account(actor), from, req.campus(), req.grades(), req.teams(),
                req.tag() == null ? "Notice" : req.tag(), req.title().trim(), req.body().trim(), req.attachmentName(),
                req.requiresAck(), req.rsvpEnabled(), link, status, calendar.now()));
        long reach = audience.guardianCount(n.getCampus(), n.getGrades(), n.getTeams());
        return new PostResult(n.getId(), status, status == PublishStatus.PENDING
                ? "Sent to the principal for approval." : "Sent to " + reach + " guardians.");
    }

    @Transactional(readOnly = true)
    public List<NoticeStats> mine(Actor actor) {
        return notices.findByAuthorIdOrderByCreatedAtDesc(actor.accountId()).stream().map(this::stats).toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeStats> allPublished() {
        return notices.findByStatusOrderByPublishedAtDesc(PublishStatus.PUBLISHED).stream().map(this::stats).toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeStats> pending() {
        return notices.findByStatusOrderByCreatedAtAsc(PublishStatus.PENDING).stream().map(this::stats).toList();
    }

    /** Teacher edits a draft that was sent back and resubmits it. */
    public PostResult resubmit(Actor actor, Long id, EditRequest req) {
        Notice n = notices.findById(id).orElseThrow(() -> new NotFoundException("Notice", id));
        if (n.getAuthor() == null || !n.getAuthor().getId().equals(actor.accountId())) {
            throw new ForbiddenException("You can only edit your own notices.");
        }
        if (n.getStatus() == PublishStatus.PUBLISHED) throw new IllegalArgumentException("This notice has already been sent.");
        n.edit(req.title().trim(), req.body().trim());
        n.resubmit();
        return new PostResult(n.getId(), n.getStatus(), "Resubmitted to the principal.");
    }

    // ---------------- principal ----------------

    public PostResult approve(Long id) {
        Notice n = pendingNotice(id);
        n.publish(calendar.now());
        long reach = audience.guardianCount(n.getCampus(), n.getGrades(), n.getTeams());
        return new PostResult(n.getId(), n.getStatus(), "Approved and sent to " + reach + " guardians. The teacher has been told.");
    }

    public PostResult requestChanges(Long id, String comment) {
        Notice n = pendingNotice(id);
        n.requestChanges(comment.trim());
        return new PostResult(n.getId(), n.getStatus(), "Sent back to the teacher with your comments.");
    }

    public PostResult editAndApprove(Long id, EditRequest req) {
        Notice n = pendingNotice(id);
        n.edit(req.title().trim(), req.body().trim());
        n.publish(calendar.now());
        return new PostResult(n.getId(), n.getStatus(), "Edited, approved and sent.");
    }

    // ---------------- helpers ----------------

    static boolean matches(Notice n, Learner l) {
        return Audience.matches(l, n.getCampus(), n.getGrades(), n.getTeams());
    }

    NoticeStats stats(Notice n) {
        long guardians = audience.guardianCount(n.getCampus(), n.getGrades(), n.getTeams());
        long reads = receipts.countByNoticeIdAndType(n.getId(), NoticeReceipt.Type.READ);
        long acks = receipts.countByNoticeIdAndType(n.getId(), NoticeReceipt.Type.ACK);
        long ackTarget = n.isRequiresAck() ? audience.learners(n.getCampus(), n.getGrades(), n.getTeams()).size() : 0;
        return new NoticeStats(n.getId(), n.getFromLabel(), n.getTag(), n.getTitle(), n.getBody(), n.audienceText(),
                n.getStatus(), n.getReviewComment(), n.getCreatedAt(), n.getPublishedAt(), guardians, reads,
                Money.pct(reads, guardians), n.isRequiresAck(), acks, Money.pct(acks, ackTarget));
    }

    private Notice pendingNotice(Long id) {
        Notice n = notices.findById(id).orElseThrow(() -> new NotFoundException("Notice", id));
        if (n.getStatus() != PublishStatus.PENDING) throw new IllegalArgumentException("This notice isn't waiting for approval.");
        return n;
    }

    private Notice visibleTo(Actor actor, Long id) {
        Notice n = notices.findById(id).filter(x -> x.getStatus() == PublishStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Notice", id));
        if (actor.is(Role.PARENT) && access.myLearners(actor).stream().noneMatch(k -> matches(n, k))) {
            throw new NotFoundException("Notice", id);
        }
        return n;
    }

    private void read(Notice n, Actor actor) {
        if (!receipts.existsByNoticeIdAndAccountIdAndType(n.getId(), actor.accountId(), NoticeReceipt.Type.READ)) {
            receipts.save(new NoticeReceipt(n, account(actor), null, NoticeReceipt.Type.READ, null, calendar.now()));
        }
    }

    private UserAccount account(Actor actor) { return wallets.managed(actor.accountId()); }

    private NoticeView view(Notice n, Long accountId, List<Learner> forKids) {
        Set<Long> acked = new HashSet<>(receipts.ackedLearnerIds(n.getId()));
        Boolean rsvp = receipts.findByNoticeIdAndAccountIdAndType(n.getId(), accountId, NoticeReceipt.Type.RSVP)
                .map(NoticeReceipt::getAttending).orElse(null);
        return new NoticeView(n.getId(), n.getFromLabel(), n.getTag(), n.getTitle(), n.getBody(), n.getAttachmentName(),
                n.audienceText(), n.getPublishedAt(),
                receipts.existsByNoticeIdAndAccountIdAndType(n.getId(), accountId, NoticeReceipt.Type.READ),
                n.isRequiresAck(),
                forKids.stream().map(k -> new ChildAck(k.getId(), k.getFirstName(), acked.contains(k.getId()))).toList(),
                n.isRsvpEnabled(), rsvp, n.getLinkedCollection() == null ? null : n.getLinkedCollection().getId());
    }
}
