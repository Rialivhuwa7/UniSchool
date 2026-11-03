package co.za.unicard.repository;

import co.za.unicard.model.LedgerSource;
import co.za.unicard.model.SchoolLedgerEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SchoolLedgerRepository extends JpaRepository<SchoolLedgerEntry, Long> {

    @Query("select e from SchoolLedgerEntry e where e.source <> co.za.unicard.model.LedgerSource.SHOP_GIVEBACK order by e.at desc, e.id desc")
    List<SchoolLedgerEntry> latestPayments(Pageable page);

    @Query("select coalesce(sum(e.amount), 0) from SchoolLedgerEntry e where e.source = :source and e.at >= :from")
    BigDecimal totalSince(@Param("source") LedgerSource source, @Param("from") LocalDateTime from);

    List<SchoolLedgerEntry> findByAtGreaterThanEqualOrderByAtDesc(LocalDateTime from);
}
