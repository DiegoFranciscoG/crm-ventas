package ec.dfgranda.crm.service;

import ec.dfgranda.crm.domain.ForecastCategory;
import ec.dfgranda.crm.domain.Opportunity;
import ec.dfgranda.crm.domain.PipelineStage;
import ec.dfgranda.crm.dto.ForecastResponse;
import ec.dfgranda.crm.exception.BusinessRuleException;
import ec.dfgranda.crm.repository.OpportunityRepository;
import ec.dfgranda.crm.repository.PipelineStageRepository;
import ec.dfgranda.crm.repository.Specs;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Forecast de ventas (investigación #9, #10):
 * pipeline = Σ monto abierto; ponderado = Σ monto × probabilidad; commit = Σ monto en categoría COMMIT;
 * best case = commit + Σ monto en BEST_CASE; ganado = Σ monto cerrado ganado en el período;
 * tasa de cierre = ganadas / (ganadas + perdidas) en el período.
 */
@Service
public class ForecastService {

    private static final int MAX_MONTHS = 24;

    private final OpportunityRepository opportunities;
    private final PipelineStageRepository stages;
    private final AccessPolicy accessPolicy;

    public ForecastService(OpportunityRepository opportunities, PipelineStageRepository stages, AccessPolicy accessPolicy) {
        this.opportunities = opportunities;
        this.stages = stages;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public ForecastResponse forecast(LocalDate from, LocalDate to, Long ownerId) {
        if (to.isBefore(from)) {
            throw new BusinessRuleException("La fecha final debe ser posterior a la inicial.");
        }
        if (YearMonth.from(from).plusMonths(MAX_MONTHS).isBefore(YearMonth.from(to))) {
            throw new BusinessRuleException("El período máximo del forecast es de " + MAX_MONTHS + " meses.");
        }
        Long scope = accessPolicy.ownerScope();
        Long ownerFilter = scope != null ? scope : ownerId;
        Instant fromInstant = from.atStartOfDay(OpportunityService.BUSINESS_ZONE).toInstant();
        Instant toExclusive = to.plusDays(1).atStartOfDay(OpportunityService.BUSINESS_ZONE).toInstant();
        List<Opportunity> relevant = opportunities.findAll(
                Specs.all(Specs.opportunityOwnedBy(ownerFilter), Specs.relevantForPeriod(from, to, fromInstant, toExclusive)),
                Sort.by("id"));
        return new ForecastResponse(from, to, ownerFilter, totals(relevant), byMonth(relevant, from, to),
                byStage(relevant), scope == null ? byOwner(relevant) : List.of());
    }

    static ForecastResponse.Totals totals(List<Opportunity> relevant) {
        BigDecimal pipeline = BigDecimal.ZERO;
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal commit = BigDecimal.ZERO;
        BigDecimal bestCaseOnly = BigDecimal.ZERO;
        BigDecimal won = BigDecimal.ZERO;
        BigDecimal lost = BigDecimal.ZERO;
        int open = 0;
        int wonCount = 0;
        int lostCount = 0;
        for (Opportunity o : relevant) {
            PipelineStage stage = o.getStage();
            if (!stage.isClosed()) {
                open++;
                pipeline = pipeline.add(o.getAmount());
                weighted = weighted.add(o.weightedAmount());
                if (stage.getForecastCategory() == ForecastCategory.COMMIT) {
                    commit = commit.add(o.getAmount());
                } else if (stage.getForecastCategory() == ForecastCategory.BEST_CASE) {
                    bestCaseOnly = bestCaseOnly.add(o.getAmount());
                }
            } else if (stage.isWon()) {
                wonCount++;
                won = won.add(o.getAmount());
            } else {
                lostCount++;
                lost = lost.add(o.getAmount());
            }
        }
        BigDecimal winRate = wonCount + lostCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(wonCount * 100L).divide(BigDecimal.valueOf(wonCount + lostCount), 1, RoundingMode.HALF_UP);
        return new ForecastResponse.Totals(pipeline, weighted, commit, commit.add(bestCaseOnly), won, lost, winRate,
                open, wonCount, lostCount);
    }

    private static List<ForecastResponse.Monthly> byMonth(List<Opportunity> relevant, LocalDate from, LocalDate to) {
        Map<YearMonth, List<Opportunity>> buckets = new LinkedHashMap<>();
        for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
            buckets.put(month, new ArrayList<>());
        }
        for (Opportunity o : relevant) {
            YearMonth month = o.getStage().isClosed()
                    ? YearMonth.from(o.getClosedAt().atZone(OpportunityService.BUSINESS_ZONE))
                    : YearMonth.from(o.getExpectedCloseDate());
            List<Opportunity> bucket = buckets.get(month);
            if (bucket != null) {
                bucket.add(o);
            }
        }
        List<ForecastResponse.Monthly> result = new ArrayList<>();
        buckets.forEach((month, list) -> {
            ForecastResponse.Totals t = totals(list);
            result.add(new ForecastResponse.Monthly(month.toString(), t.openPipeline(), t.weightedPipeline(), t.commit(),
                    t.bestCase(), t.closedWon()));
        });
        return result;
    }

    private List<ForecastResponse.ByStage> byStage(List<Opportunity> relevant) {
        List<ForecastResponse.ByStage> result = new ArrayList<>();
        for (PipelineStage stage : stages.findAllByOrderByPositionAsc()) {
            List<Opportunity> inStage = relevant.stream().filter(o -> o.getStage().getCode().equals(stage.getCode())).toList();
            result.add(new ForecastResponse.ByStage(stage.getCode(), stage.getName(), inStage.size(),
                    inStage.stream().map(Opportunity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                    inStage.stream().filter(o -> !o.getStage().isClosed()).map(Opportunity::weightedAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
        return result;
    }

    private static List<ForecastResponse.ByOwner> byOwner(List<Opportunity> relevant) {
        Map<Long, List<Opportunity>> grouped = new LinkedHashMap<>();
        relevant.forEach(o -> grouped.computeIfAbsent(o.getOwner().getId(), k -> new ArrayList<>()).add(o));
        return grouped.values().stream().map(list -> {
            ForecastResponse.Totals t = totals(list);
            return new ForecastResponse.ByOwner(list.getFirst().getOwner().getId(), list.getFirst().getOwner().getFullName(),
                    t.openCount(), t.weightedPipeline(), t.closedWon());
        }).sorted((a, b) -> b.weighted().compareTo(a.weighted())).toList();
    }
}
