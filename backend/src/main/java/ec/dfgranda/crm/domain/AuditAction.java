package ec.dfgranda.crm.domain;

/** Acciones registradas en el log de accesos a datos personales. */
public enum AuditAction {
    VIEW, LIST, CREATE, UPDATE, EXPORT, ERASE, CONSENT_GRANT, CONSENT_REVOKE, PDF_DOWNLOAD
}
