package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QuoteSummary(Long id, String number, Long opportunityId, String opportunityTitle, String companyName,
                           QuoteStatus status, LocalDate issueDate, LocalDate validUntil, BigDecimal total) {
}
