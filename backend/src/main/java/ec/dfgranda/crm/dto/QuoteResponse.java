package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.QuoteStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record QuoteResponse(Long id, String number, Long opportunityId, String opportunityTitle, Long companyId,
                            String companyName, Long contactId, String contactName, QuoteStatus status,
                            LocalDate issueDate, LocalDate validUntil, String currency, BigDecimal subtotal,
                            BigDecimal discountTotal, BigDecimal taxTotal, BigDecimal total, List<TaxLine> taxBreakdown,
                            String notes, List<QuoteItemResponse> items, String createdBy, Instant createdAt) {
}
