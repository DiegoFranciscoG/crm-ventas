package ec.dfgranda.crm.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OpportunityResponse(Long id, String title, Long companyId, String companyName, Long primaryContactId,
                                  String primaryContactName, UserSummary owner, String stageCode, String stageName,
                                  BigDecimal amount, int probability, BigDecimal weightedAmount,
                                  LocalDate expectedCloseDate, Instant closedAt, String lostReason,
                                  String description, long version, Instant createdAt, Instant updatedAt) {
}
