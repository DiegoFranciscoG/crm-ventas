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

/** Finalidad del tratamiento con su base legal y plazo de conservación (LOPDP Art. 7-8, Reglamento Art. 10). */
@Entity
@Immutable
@Table(name = "processing_purposes")
public class ProcessingPurpose {

    public static final String COMMERCIAL_CONTACT = "COMMERCIAL_CONTACT";
    public static final String QUOTE_DELIVERY = "QUOTE_DELIVERY";
    public static final String EMAIL_MARKETING = "EMAIL_MARKETING";

    @Id
    private String code;

    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_basis")
    private LegalBasis legalBasis;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "retention_months")
    private Integer retentionMonths;

    protected ProcessingPurpose() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LegalBasis getLegalBasis() {
        return legalBasis;
    }

    public Integer getRetentionMonths() {
        return retentionMonths;
    }
}
