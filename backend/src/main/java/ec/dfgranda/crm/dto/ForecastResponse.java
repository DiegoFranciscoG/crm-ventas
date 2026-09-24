package ec.dfgranda.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Dashboard de forecast. Fórmulas en docs/modelo-datos.md (investigación #9, #10). */
public record ForecastResponse(LocalDate from, LocalDate to, Long ownerId, Totals totals, List<Monthly> byMonth,
                               List<ByStage> byStage, List<ByOwner> byOwner) {

    public record Totals(BigDecimal openPipeline, BigDecimal weightedPipeline, BigDecimal commit,
                         BigDecimal bestCase, BigDecimal closedWon, BigDecimal closedLost, BigDecimal winRate,
                         int openCount, int wonCount, int lostCount) {
    }

    public record Monthly(String month, BigDecimal pipeline, BigDecimal weighted, BigDecimal commit,
                          BigDecimal bestCase, BigDecimal closedWon) {
    }

    public record ByStage(String stageCode, String stageName, int count, BigDecimal amount, BigDecimal weighted) {
    }

    public record ByOwner(Long ownerId, String ownerName, int openCount, BigDecimal weighted, BigDecimal closedWon) {
    }
}
