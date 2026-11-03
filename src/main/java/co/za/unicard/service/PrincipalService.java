package co.za.unicard.service;

import co.za.unicard.model.SchoolSettings;
import co.za.unicard.repository.SchoolSettingsRepository;
import co.za.unicard.util.Money;
import co.za.unicard.dto.HomeworkDtos.HomeworkStats;
import co.za.unicard.dto.NoticeDtos.NoticeStats;
import co.za.unicard.dto.SchoolDtos.*;
import co.za.unicard.dto.TuckshopDtos.TillSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class PrincipalService {

    private static final int LOW_READ_PERCENT = 75;

    private final SchoolOfficeService office;
    private final NoticeService notices;
    private final HomeworkService homework;
    private final GroupService groups;
    private final PosService pos;
    private final SchoolSettingsRepository settings;
    private final SchoolCalendar calendar;

    public PrincipalService(SchoolOfficeService office, NoticeService notices, HomeworkService homework, GroupService groups,
                            PosService pos, SchoolSettingsRepository settings, SchoolCalendar calendar) {
        this.office = office;
        this.notices = notices;
        this.homework = homework;
        this.groups = groups;
        this.pos = pos;
        this.settings = settings;
        this.calendar = calendar;
    }

    public PrincipalDashboard dashboard() {
        Overview o = office.overview();
        List<NoticeStats> ns = notices.allPublished();
        List<HomeworkStats> hs = homework.published();
        long reads = ns.stream().mapToLong(NoticeStats::reads).sum();
        long reach = ns.stream().mapToLong(NoticeStats::guardians).sum();
        long done = hs.stream().mapToLong(HomeworkStats::done).sum();
        long set = hs.stream().mapToLong(HomeworkStats::total).sum();

        List<Attention> attention = new ArrayList<>();
        long reported = groups.reportedCount();
        if (reported > 0) attention.add(new Attention("urgent", reported + " group message" + (reported > 1 ? "s" : "") + " reported by parents", "groups"));
        ns.stream()
          .filter(n -> n.publishedAt() != null && n.publishedAt().toLocalDate().isBefore(calendar.today()) && n.readPercent() < LOW_READ_PERCENT)
          .forEach(n -> attention.add(new Attention("follow-up", "\"" + n.title() + "\" read by only " + n.readPercent() + "% of parents", "notices")));
        FeePlanBreakdown f = office.feePlans();
        if (f.withoutPlan() > 0) attention.add(new Attention("follow-up", f.withoutPlan() + " learners have no " + f.year() + " fee plan yet", "fees"));
        long low = office.uniform().lowSizes();
        if (low > 0) attention.add(new Attention("note", low + " uniform sizes running low in the school shop", "uniform"));

        TillSummary till = pos.today();
        return new PrincipalDashboard(o.feePlanPercent(), o.collected(), Money.pct(reads, reach), Money.pct(done, set),
                approvals(), settingsView(), attention, till.preOrders(), till.collectedInFull(), till.salesToday(), hs, ns);
    }

    @Transactional(readOnly = true)
    public List<ApprovalItem> approvals() {
        List<ApprovalItem> items = new ArrayList<>();
        notices.pending().forEach(n -> items.add(new ApprovalItem("NOTICE", n.id(), n.from(), n.title(), n.body(), n.audience(), n.createdAt())));
        homework.pending().forEach(h -> items.add(new ApprovalItem("HOMEWORK", h.id(), h.by() + ", " + h.subject(), h.title(), h.body(),
                h.grade() + ", " + h.school(), h.createdAt())));
        items.sort(Comparator.comparing(ApprovalItem::submittedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    @Transactional(readOnly = true)
    public SettingsView settingsView() {
        SchoolSettings s = settings.current();
        return new SettingsView(s.isApproveNotices(), s.isApproveHomework());
    }

    public SettingsView updateSettings(SettingsUpdate req) {
        SchoolSettings s = settings.current();
        if (req.approveNotices() != null) s.setApproveNotices(req.approveNotices());
        if (req.approveHomework() != null) s.setApproveHomework(req.approveHomework());
        return new SettingsView(s.isApproveNotices(), s.isApproveHomework());
    }
}
