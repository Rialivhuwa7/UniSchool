package co.za.unicard.repository;

import co.za.unicard.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Query("select t from Ticket t join fetch t.collection where t.owner.id = :accountId order by t.id")
    List<Ticket> findByOwner(@Param("accountId") Long accountId);
}
