package ec.dfgranda.crm.mapper;

import ec.dfgranda.crm.domain.AccessAuditLog;
import ec.dfgranda.crm.domain.DataSubjectRequest;
import ec.dfgranda.crm.dto.AuditLogResponse;
import ec.dfgranda.crm.dto.DataSubjectRequestResponse;

public final class PrivacyMapper {

    private PrivacyMapper() {
    }

    public static DataSubjectRequestResponse toResponse(DataSubjectRequest r) {
        return new DataSubjectRequestResponse(r.getId(), r.getContactId(), r.getRequestType(), r.getChannel(),
                r.getReceivedAt(), r.getDueAt(), r.getStatus(), r.getCompletedAt(), r.getResolution(),
                r.getHandledBy().getFullName());
    }

    public static AuditLogResponse toResponse(AccessAuditLog log, String userName) {
        return new AuditLogResponse(log.getId(), log.getOccurredAt(), log.getUserId(), userName, log.getAction(),
                log.getResourceType(), log.getResourceId(), log.isSuccess(), log.getIpAddress(), log.getDetail());
    }
}
