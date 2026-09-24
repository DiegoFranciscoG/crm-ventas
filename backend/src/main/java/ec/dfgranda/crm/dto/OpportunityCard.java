package ec.dfgranda.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpportunityCard(Long id, String title, Long companyId, String companyName, UserSummary owner,
                              BigDecimal amount, int probability, LocalDate expectedCloseDate, boolean overdue,
                              long version) {
}
