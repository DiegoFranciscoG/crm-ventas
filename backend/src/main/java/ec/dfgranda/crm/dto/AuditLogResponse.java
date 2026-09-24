package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.AuditResourceType;

import java.time.Instant;

public record AuditLogResponse(Long id, Instant occurredAt, Long userId, String userName, AuditAction action,
                               AuditResourceType resourceType, Long resourceId, boolean success, String ipAddress,
                               String detail) {
}
