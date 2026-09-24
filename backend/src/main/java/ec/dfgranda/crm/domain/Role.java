package ec.dfgranda.crm.domain;

/** Roles del sistema: el vendedor ve lo suyo, el gerente y el administrador ven todo. */
public enum Role {
    ADMIN, MANAGER, SALES_REP;

    /** Gerente y administrador tienen visibilidad global; el vendedor solo sobre sus registros. */
    public boolean hasGlobalVisibility() {
        return this != SALES_REP;
    }
}
