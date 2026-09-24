package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.RequestChannel;
import ec.dfgranda.crm.domain.RequestStatus;
import ec.dfgranda.crm.domain.RequestType;

import java.time.Instant;

public record DataSubjectRequestResponse(Long id, Long contactId, RequestType requestType, RequestChannel channel,
                                         Instant receivedAt, Instant dueAt, RequestStatus status,
                                         Instant completedAt, String resolution, String handledBy) {
}
