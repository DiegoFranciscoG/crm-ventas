package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Debe indicar una empresa o una oportunidad. El contacto debe pertenecer a esa empresa. */
public record ActivityRequest(
        @NotNull ActivityType type,
        @NotBlank @Size(max = 200) String subject,
        @Size(max = 4000) String notes,
        Instant dueAt,
        Instant remindAt,
        Long companyId,
        Long contactId,
        Long opportunityId) {
}
