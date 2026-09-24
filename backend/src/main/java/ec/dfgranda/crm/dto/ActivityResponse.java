package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.ActivityType;

import java.time.Instant;

public record ActivityResponse(Long id, ActivityType type, String subject, String notes, Instant dueAt,
                               Instant remindAt, Instant reminderSentAt, Instant completedAt, UserSummary owner,
                               Long companyId, String companyName, Long contactId, String contactName,
                               Long opportunityId, String opportunityTitle, boolean overdue) {
}
