package ec.dfgranda.crm.mapper;

import ec.dfgranda.crm.domain.Activity;
import ec.dfgranda.crm.dto.ActivityResponse;

import java.time.Instant;

public final class ActivityMapper {

    private ActivityMapper() {
    }

    public static ActivityResponse toResponse(Activity a, Instant now) {
        boolean overdue = a.getCompletedAt() == null && a.getDueAt() != null && a.getDueAt().isBefore(now);
        return new ActivityResponse(a.getId(), a.getType(), a.getSubject(), a.getNotes(), a.getDueAt(),
                a.getRemindAt(), a.getReminderSentAt(), a.getCompletedAt(), UserMapper.toSummary(a.getOwner()),
                a.getCompany() == null ? null : a.getCompany().getId(),
                a.getCompany() == null ? null : a.getCompany().getLegalName(),
                a.getContact() == null ? null : a.getContact().getId(),
                a.getContact() == null ? null : a.getContact().fullName(),
                a.getOpportunity() == null ? null : a.getOpportunity().getId(),
                a.getOpportunity() == null ? null : a.getOpportunity().getTitle(),
                overdue);
    }
}
