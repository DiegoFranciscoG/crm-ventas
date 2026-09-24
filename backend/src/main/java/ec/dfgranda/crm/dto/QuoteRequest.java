package ec.dfgranda.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record QuoteRequest(
        @NotNull Long opportunityId,
        Long contactId,
        LocalDate issueDate,
        LocalDate validUntil,
        @Size(max = 2000) String notes,
        @NotEmpty @Size(max = 50) List<@Valid @NotNull QuoteItemRequest> items) {
}
