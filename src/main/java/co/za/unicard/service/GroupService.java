package co.za.unicard.service;

import co.za.unicard.exception.ForbiddenException;
import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.GroupMembership;
import co.za.unicard.model.GroupMessage;
import co.za.unicard.model.Learner;
import co.za.unicard.model.ParentGroup;
import co.za.unicard.model.Role;
import co.za.unicard.model.RoleAssignment;
import co.za.unicard.repository.GroupMembershipRepository;
import co.za.unicard.repository.GroupMessageRepository;
import co.za.unicard.repository.ParentGroupRepository;
import co.za.unicard.security.Actor;
import co.za.unicard.dto.GroupDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class GroupService {

    private final ParentGroupRepository groups;
    private final GroupMessageRepository messages;
    private final GroupMembershipRepository memberships;
    private final AudienceService audience;
    private final LearnerAccess access;
    private final WalletService wallets;
    private final SchoolCalendar calendar;

    public GroupService(ParentGroupRepository groups, GroupMessageRepository messages, GroupMembershipRepository memberships,
                        AudienceService audience, LearnerAccess access, WalletService wallets, SchoolCalendar calendar) {
        this.groups = groups;
        this.messages = messages;
        this.memberships = memberships;
        this.audience = audience;
        this.access = access;
        this.wallets = wallets;
        this.calendar = calendar;
    }

    /** Parents: groups for their children's grades and teams. Teachers: their class's grade group. */
    @Transactional(readOnly = true)
    public List<GroupView> myGroups(Actor actor) {
        List<GroupView> out = new ArrayList<>();
        List<Learner> kids = access.myLearners(actor);
        for (ParentGroup g : groups.findAll()) {
            List<String> forKids = kids.stream().filter(g::includes).map(Learner::getFirstName).toList();
            if (forKids.isEmpty() && !isTeacherOf(actor, g)) continue;
            var m = memberships.findByGroupIdAndAccountId(g.getId(), actor.accountId());
            LocalDateTime lastRead = m.map(GroupMembership::getLastReadAt).orElse(null);
            LocalDateTime now = calendar.now();
            long unread = messages.findByGroupIdAndHiddenFalseOrderBySentAtAscIdAsc(g.getId()).stream()
                    .filter(x -> !x.isBy(actor.accountId()) && !x.getDeliverAt().isAfter(now)
                            && (lastRead == null || x.getDeliverAt().isAfter(lastRead)))
                    .count();
            out.add(new GroupView(g.getId(), g.getName(), g.getCampus(), g.getCampus().displayName(), forKids,
                    audience.guardianCount(g.getCampus(), g.grades(), g.teams()), g.getModeratorLabel(), g.getMode(),
                    canPost(actor, g), m.map(GroupMembership::isMuted).orElse(false), unread));
        }
        return out;
    }

    /** Messages visible to this person; opening the group marks it read. */
    public List<MessageView> messages(Actor actor, Long groupId) {
        ParentGroup g = member(actor, groupId);
        LocalDateTime now = calendar.now();
        List<MessageView> out = messages.findByGroupIdAndHiddenFalseOrderBySentAtAscIdAsc(g.getId()).stream()
                .filter(m -> m.isBy(actor.accountId()) || !m.getDeliverAt().isAfter(now))
                .map(m -> view(m, actor.accountId(), now)).toList();
        membership(actor, g).setLastReadAt(now);
        return out;
    }

    public PostMessageResult post(Actor actor, Long groupId, String body) {
        ParentGroup g = member(actor, groupId);
        if (!canPost(actor, g)) throw new ForbiddenException("Only the moderator can post in " + g.getName() + ".");
        LocalDateTime now = calendar.now();
        LocalDateTime deliver = calendar.deliveryTimeFor(now);
        String role = actor.is(Role.TEACHER) ? "Teacher" : g.isModerator(actor.accountId()) ? "Moderator" : null;
        String name = actor.is(Role.TEACHER) ? actor.account().getDisplayName() : actor.account().getFirstName();
        GroupMessage m = messages.save(new GroupMessage(g, wallets.managed(actor.accountId()), name, role, body.trim(), now, deliver));
        membership(actor, g).setLastReadAt(now);
        String notice = deliver.isAfter(now) ? "Quiet hours: your message will arrive at 06:00." : null;
        return new PostMessageResult(view(m, actor.accountId(), now), notice);
    }

    public void report(Actor actor, Long messageId) {
        GroupMessage m = messages.findById(messageId).orElseThrow(() -> new NotFoundException("Message", messageId));
        member(actor, m.getGroup().getId());
        if (m.isBy(actor.accountId())) throw new IllegalArgumentException("You can't report your own message.");
        m.report();
    }

    public GroupView mute(Actor actor, Long groupId, boolean muted) {
        ParentGroup g = member(actor, groupId);
        membership(actor, g).setMuted(muted);
        return myGroups(actor).stream().filter(v -> v.id().equals(g.getId())).findFirst().orElseThrow();
    }

    // ---- school office ----
    @Transactional(readOnly = true)
    public List<GroupAdminView> allGroups() {
        List<GroupMessage> reported = messages.findByReportsGreaterThanAndHiddenFalseOrderBySentAtDesc(0);
        return groups.findAll().stream().map(g -> new GroupAdminView(g.getId(), g.getName(), g.getCampus(),
                audience.guardianCount(g.getCampus(), g.grades(), g.teams()), g.getModeratorLabel(), g.getMode(),
                reported.stream().filter(m -> m.getGroup().getId().equals(g.getId())).count())).toList();
    }

    public GroupAdminView setMode(Long groupId, ParentGroup.Mode mode) {
        ParentGroup g = groups.findById(groupId).orElseThrow(() -> new NotFoundException("Group", groupId));
        g.setMode(mode);
        return allGroups().stream().filter(v -> v.id().equals(groupId)).findFirst().orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<ReportedMessage> reported() {
        return messages.findByReportsGreaterThanAndHiddenFalseOrderBySentAtDesc(0).stream()
                .map(m -> new ReportedMessage(m.getId(), m.getGroup().getId(), m.getGroup().getName(), m.getAuthorName(),
                        m.getBody(), m.getReports(), m.getSentAt())).toList();
    }

    public void hide(Long messageId) {
        messages.findById(messageId).orElseThrow(() -> new NotFoundException("Message", messageId)).hide();
    }

    @Transactional(readOnly = true)
    public long reportedCount() {
        return messages.countByReportsGreaterThanAndHiddenFalse(0);
    }

    // ---- helpers ----
    private ParentGroup member(Actor actor, Long groupId) {
        ParentGroup g = groups.findById(groupId).orElseThrow(() -> new NotFoundException("Group", groupId));
        boolean ok = access.myLearners(actor).stream().anyMatch(g::includes) || isTeacherOf(actor, g);
        if (!ok) throw new ForbiddenException("You're not in this group.");
        return g;
    }

    private boolean isTeacherOf(Actor actor, ParentGroup g) {
        RoleAssignment r = actor.roleAssignment();
        return actor.is(Role.TEACHER) && g.getGrade() != null && g.getGrade().equals(r.getGrade())
                && r.getCampus() != null && g.getCampus().includes(r.getCampus());
    }

    private boolean canPost(Actor actor, ParentGroup g) {
        return g.getMode() == ParentGroup.Mode.OPEN || g.isModerator(actor.accountId()) || isTeacherOf(actor, g);
    }

    private GroupMembership membership(Actor actor, ParentGroup g) {
        return memberships.findByGroupIdAndAccountId(g.getId(), actor.accountId())
                .orElseGet(() -> memberships.save(new GroupMembership(g, wallets.managed(actor.accountId()), null)));
    }

    private static MessageView view(GroupMessage m, Long me, LocalDateTime now) {
        return new MessageView(m.getId(), m.isBy(me) ? "You" : m.getAuthorName(), m.getAuthorRole(), m.getBody(),
                m.getSentAt(), m.isBy(me), m.getDeliverAt().isAfter(now), m.getDeliverAt());
    }
}
