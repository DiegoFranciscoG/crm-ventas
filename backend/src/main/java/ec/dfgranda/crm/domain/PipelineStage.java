package ec.dfgranda.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Etapa del pipeline con probabilidad y categoría de forecast por defecto (investigación #9). */
@Entity
@Immutable
@Table(name = "pipeline_stages")
public class PipelineStage {

    public static final String CLOSED_WON = "CLOSED_WON";
    public static final String CLOSED_LOST = "CLOSED_LOST";

    @Id
    private String code;

    private String name;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer position;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "default_probability")
    private Integer defaultProbability;

    @Enumerated(EnumType.STRING)
    @Column(name = "forecast_category")
    private ForecastCategory forecastCategory;

    private boolean closed;

    private boolean won;

    protected PipelineStage() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getPosition() {
        return position;
    }

    public Integer getDefaultProbability() {
        return defaultProbability;
    }

    public ForecastCategory getForecastCategory() {
        return forecastCategory;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isWon() {
        return won;
    }

    public boolean isLost() {
        return closed && !won;
    }
}
