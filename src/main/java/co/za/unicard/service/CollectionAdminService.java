package co.za.unicard.service;

import co.za.unicard.exception.NotFoundException;
import co.za.unicard.model.SchoolCollection;
import co.za.unicard.repository.CollectionPaymentRepository;
import co.za.unicard.repository.SchoolCollectionRepository;
import co.za.unicard.util.Money;
import co.za.unicard.dto.PaymentDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** School office: create collections, track who has paid, send reminders, export. */
@Service
@Transactional
public class CollectionAdminService {

    private final SchoolCollectionRepository collections;
    private final CollectionPaymentRepository payments;
    private final AudienceService audience;
    private final SchoolCalendar calendar;

    public CollectionAdminService(SchoolCollectionRepository collections, CollectionPaymentRepository payments,
                                  AudienceService audience, SchoolCalendar calendar) {
        this.collections = collections;
        this.payments = payments;
        this.audience = audience;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public List<CollectionStats> all() {
        return collections.findAllByOrderByCreatedAtDesc().stream().map(this::stats).toList();
    }

    public CollectionStats create(NewCollectionRequest req) {
        SchoolCollection k = collections.save(new SchoolCollection(req.title().trim(), Money.round(req.amount()), req.kind(),
                req.campus(), req.grades(), req.dueDate(), req.instalments() == null ? 1 : req.instalments(), calendar.now()));
        return stats(k);
    }

    /**
     * Reminds families who haven't paid. Delivery (push, SMS, email) is not built yet; this
     * returns how many families would be reminded.
     */
    @Transactional(readOnly = true)
    public ReminderResult remind(Long id) {
        SchoolCollection k = collections.findById(id).orElseThrow(() -> new NotFoundException("Collection", id));
        CollectionStats s = stats(k);
        long n = s.eligible() - s.paid();
        return new ReminderResult(n, k.getKind() == SchoolCollection.Kind.PAYMENT
                ? "Reminder sent to " + n + " families who haven't paid."
                : "Reminder sent to " + n + " families.");
    }

    @Transactional(readOnly = true)
    public String csv() {
        StringBuilder sb = new StringBuilder("Collection,Type,Amount,Due,Paid,Eligible,Collected\n");
        for (CollectionStats s : all()) {
            sb.append(csvCell(s.title())).append(',').append(s.kind()).append(',').append(s.amount()).append(',')
              .append(s.dueDate() == null ? "Open" : s.dueDate()).append(',').append(s.paid()).append(',')
              .append(s.eligible()).append(',').append(s.collected()).append('\n');
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public BigDecimal totalCollected() {
        return Money.round(payments.totalCollected());
    }

    CollectionStats stats(SchoolCollection k) {
        long eligible = audience.learners(k.getCampus(), k.getGrades(), List.of()).size();
        long paid = payments.learnersPaid(k.getId());
        BigDecimal collected = Money.round(payments.totalFor(k.getId()));
        return new CollectionStats(k.getId(), k.getTitle(), k.getKind(), k.getAmount(), k.getCampus(), k.getGrades(),
                k.getDueDate(), k.getInstalments(), eligible, paid, Money.pct(paid, eligible), collected);
    }

    private static String csvCell(String v) {
        return v.contains(",") || v.contains("\"") || v.contains("\n") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}
