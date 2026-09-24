package ec.dfgranda.crm.repository;

import ec.dfgranda.crm.domain.AccessAuditLog;
import ec.dfgranda.crm.domain.AuditResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AccessAuditLogRepository extends JpaRepository<AccessAuditLog, Long>, JpaSpecificationExecutor<AccessAuditLog> {

    List<AccessAuditLog> findByResourceTypeAndResourceIdOrderByOccurredAtAsc(AuditResourceType resourceType, Long resourceId);
}
