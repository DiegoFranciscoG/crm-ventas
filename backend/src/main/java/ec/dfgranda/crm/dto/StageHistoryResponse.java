package ec.dfgranda.crm.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StageHistoryResponse(String fromStage, String toStage, BigDecimal amount, int probability,
                                   String changedBy, Instant changedAt) {
}
