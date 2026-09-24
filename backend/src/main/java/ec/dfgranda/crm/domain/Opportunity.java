package ec.dfgranda.crm.domain;

import ec.dfgranda.crm.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "opportunities")
public class Opportunity extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_contact_id")
    private Contact primaryContact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_code", nullable = false)
    private PipelineStage stage;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private Integer probability;

    @Column(name = "expected_close_date", nullable = false)
    private LocalDate expectedCloseDate;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "lost_reason", length = 300)
    private String lostReason;

    @Column(length = 2000)
    private String description;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Opportunity() {
    }

    public Opportunity(String title, Company company, User owner, PipelineStage initialStage,
                       BigDecimal amount, LocalDate expectedCloseDate) {
        this.title = title;
        this.company = company;
        this.owner = owner;
        this.stage = initialStage;
        this.probability = initialStage.getDefaultProbability();
        this.amount = amount;
        this.expectedCloseDate = expectedCloseDate;
    }

    /**
     * Mueve la oportunidad a otra etapa: aplica la probabilidad por defecto de la etapa (investigación #9),
     * fija la fecha de cierre en etapas cerradas y exige el motivo de pérdida.
     */
    public void moveTo(PipelineStage target, String reasonIfLost, Instant now) {
        if (target.isLost() && (reasonIfLost == null || reasonIfLost.isBlank())) {
            throw new BusinessRuleException("Para marcar una oportunidad como perdida indique el motivo.");
        }
        this.stage = target;
        this.probability = target.getDefaultProbability();
        this.closedAt = target.isClosed() ? now : null;
        this.lostReason = target.isLost() ? reasonIfLost.trim() : null;
    }

    /** La probabilidad solo se ajusta a mano en etapas abiertas; en etapas cerradas es 100 o 0. */
    public void adjustProbability(int newProbability) {
        if (stage.isClosed()) {
            throw new BusinessRuleException("No se puede cambiar la probabilidad de una oportunidad cerrada.");
        }
        if (newProbability < 0 || newProbability > 100) {
            throw new BusinessRuleException("La probabilidad debe estar entre 0 y 100.");
        }
        this.probability = newProbability;
    }

    /** Monto ponderado = monto × probabilidad (investigación #10). */
    public BigDecimal weightedAmount() {
        return amount.multiply(BigDecimal.valueOf(probability))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Company getCompany() {
        return company;
    }

    public Contact getPrimaryContact() {
        return primaryContact;
    }

    public void setPrimaryContact(Contact primaryContact) {
        this.primaryContact = primaryContact;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public PipelineStage getStage() {
        return stage;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getProbability() {
        return probability;
    }

    public LocalDate getExpectedCloseDate() {
        return expectedCloseDate;
    }

    public void setExpectedCloseDate(LocalDate expectedCloseDate) {
        this.expectedCloseDate = expectedCloseDate;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public String getLostReason() {
        return lostReason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getVersion() {
        return version;
    }
}
