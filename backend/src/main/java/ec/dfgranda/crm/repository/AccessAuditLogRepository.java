package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.AccessAuditLog;
import ec.dfgranda.crm.domain.AuditResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AccessAuditLogRepository extends JpaRepository<AccessAuditLog, Long> {

    List<AccessAuditLog> findByResourceTypeAndResourceIdOrderByOccurredAtAsc(AuditResourceType resourceType, Long resourceId);

    @Query("""
            select a from AccessAuditLog a
            where (:resourceType is null or a.resourceType = :resourceType)
              and (:resourceId is null or a.resourceId = :resourceId)
              and (:userId is null or a.userId = :userId)
              and (:onlyDenied = false or a.success = false)
              and a.occurredAt >= :from and a.occurredAt < :to
            """)
    Page<AccessAuditLog> search(@Param("resourceType") AuditResourceType resourceType,
                                @Param("resourceId") Long resourceId,
                                @Param("userId") Long userId,
                                @Param("onlyDenied") boolean onlyDenied,
                                @Param("from") Instant from,
                                @Param("to") Instant to,
                                Pageable pageable);
}
