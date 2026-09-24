package ec.dfgranda.crm.exception;

import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.AuditResourceType;

/**
 * Intento de acceder a un objeto de otro vendedor. Se responde como 404 (no revela que existe) y el manejador
 * global lo registra en el log de accesos con {@code success=false}.
 */
public class ResourceAccessDeniedException extends NotFoundException {

    private final AuditAction action;
    private final AuditResourceType resourceType;
    private final Long resourceId;

    public ResourceAccessDeniedException(String message, AuditAction action, AuditResourceType resourceType, Long resourceId) {
        super(message);
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditResourceType getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }
}
