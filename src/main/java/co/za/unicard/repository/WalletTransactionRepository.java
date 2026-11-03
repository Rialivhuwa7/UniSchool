package co.za.unicard.repository;

import co.za.unicard.model.TxType;
import co.za.unicard.model.WalletTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    @Query("select t from WalletTransaction t left join t.account a left join fetch t.learner l " +
           "where a.id = :accountId or l.id in :learnerIds order by t.createdAt desc, t.id desc")
    List<WalletTransaction> activity(@Param("accountId") Long accountId,
                                     @Param("learnerIds") Collection<Long> learnerIds, Pageable page);

    @Query("select t from WalletTransaction t where t.learner.id = :learnerId order by t.createdAt desc, t.id desc")
    List<WalletTransaction> forLearner(@Param("learnerId") Long learnerId, Pageable page);

    @Query("select coalesce(sum(-t.amount), 0) from WalletTransaction t where t.type = :type and t.createdAt >= :from")
    BigDecimal totalSpentSince(@Param("type") TxType type, @Param("from") LocalDateTime from);

    @Query("select count(t) from WalletTransaction t where t.type = :type and t.createdAt >= :from")
    long countSince(@Param("type") TxType type, @Param("from") LocalDateTime from);
}
