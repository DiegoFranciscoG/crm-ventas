package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {

    List<Activity> findByContactId(Long contactId);

    @Override
    @EntityGraph(attributePaths = {"owner", "company", "contact", "opportunity"})
    Page<Activity> findAll(Specification<Activity> spec, Pageable pageable);

    /**
     * Recordatorios vencidos y pendientes, bloqueados con SKIP LOCKED: si el job interno y el cron externo corren a
     * la vez, cada recordatorio lo procesa una sola ejecución (no hay correos duplicados).
     */
    @Query(value = """
            select * from activities
            where remind_at <= :now and reminder_sent_at is null and completed_at is null
            order by remind_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<Activity> lockDueReminders(@Param("now") Instant now, @Param("limit") int limit);
}
