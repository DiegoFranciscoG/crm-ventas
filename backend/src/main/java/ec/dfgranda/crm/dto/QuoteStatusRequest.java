package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.QuoteStatus;
import jakarta.validation.constraints.NotNull;

public record QuoteStatusRequest(@NotNull QuoteStatus status) {
}
