package ec.dfgranda.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/** Historial de cambios de etapa: permite medir la velocidad del pipeline y auditar el forecast. */
@Entity
@Immutable
@Table(name = "opportunity_stage_history")
public class OpportunityStageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "opportunity_id", nullable = false)
    private Long opportunityId;

    @Column(name = "from_stage", length = 30)
    private String fromStage;

    @Column(name = "to_stage", nullable = false, length = 30)
    private String toStage;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private Integer probability;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected OpportunityStageHistory() {
    }

    public OpportunityStageHistory(Long opportunityId, String fromStage, String toStage, BigDecimal amount,
                                   Integer probability, Long changedBy, Instant changedAt) {
        this.opportunityId = opportunityId;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.amount = amount;
        this.probability = probability;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public String getFromStage() {
        return fromStage;
    }

    public String getToStage() {
        return toStage;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getProbability() {
        return probability;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
