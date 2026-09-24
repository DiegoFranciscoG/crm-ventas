package ec.dfgranda.crm.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Sección CIIU Rev. 4.0 del INEC (investigación #25). Catálogo de solo lectura. */
@Entity
@Immutable
@Table(name = "industries")
public class Industry {

    @Id
    private String code;

    private String name;

    protected Industry() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
