package ec.dfgranda.crm.dto;

import java.math.BigDecimal;
import java.util.List;

public record BoardColumn(StageResponse stage, int count, BigDecimal totalAmount, BigDecimal weightedAmount,
                          List<OpportunityCard> opportunities) {
}
