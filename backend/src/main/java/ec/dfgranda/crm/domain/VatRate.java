package ec.dfgranda.crm.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/** Tarifa de IVA con su código SRI, Tabla 17 (investigación #7, #8). Catálogo de solo lectura. */
@Entity
@Immutable
@Table(name = "vat_rates")
public class VatRate {

    @Id
    private String code;

    private String description;

    private BigDecimal percentage;

    private boolean active;

    protected VatRate() {
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public boolean isActive() {
        return active;
    }
}
