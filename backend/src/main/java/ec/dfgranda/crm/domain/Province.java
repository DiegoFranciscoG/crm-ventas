package ec.dfgranda.crm.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Provincia según la DPA del INEC (investigación #6). Catálogo de solo lectura. */
@Entity
@Immutable
@Table(name = "provinces")
public class Province {

    @Id
    private String code;

    private String name;

    protected Province() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
