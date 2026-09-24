package ec.dfgranda.crm.service;

import ec.dfgranda.crm.domain.Activity;
import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.AuditResourceType;
import ec.dfgranda.crm.domain.Company;
import ec.dfgranda.crm.domain.Contact;
import ec.dfgranda.crm.domain.Opportunity;
import ec.dfgranda.crm.domain.Quote;
import ec.dfgranda.crm.exception.NotFoundException;
import ec.dfgranda.crm.exception.ResourceAccessDeniedException;
import ec.dfgranda.crm.security.AuthenticatedUser;
import ec.dfgranda.crm.security.CurrentUserProvider;
import org.springframework.stereotype.Component;

/**
 * Autorización a nivel de objeto (OWASP API1:2023): el vendedor solo accede a lo que le pertenece; gerente y
 * administrador ven todo. Un acceso ajeno se trata como "no encontrado" para no revelar la existencia del objeto.
 */
@Component
public class AccessPolicy {

    private final CurrentUserProvider currentUser;

    public AccessPolicy(CurrentUserProvider currentUser) {
        this.currentUser = currentUser;
    }

    public AuthenticatedUser user() {
        return currentUser.get();
    }

    /** ID del dueño por el que se deben filtrar las consultas; {@code null} = sin filtro (visibilidad global). */
    public Long ownerScope() {
        AuthenticatedUser user = currentUser.get();
        return user.hasGlobalVisibility() ? null : user.id();
    }

    public boolean canAccessOwnedBy(Long ownerId) {
        AuthenticatedUser user = currentUser.get();
        return user.hasGlobalVisibility() || user.id().equals(ownerId);
    }

    public void checkCompany(Company company, AuditAction action) {
        if (!canAccessOwnedBy(company.getOwner().getId())) {
            throw new ResourceAccessDeniedException("Empresa no encontrada.", action, AuditResourceType.COMPANY, company.getId());
        }
    }

    public void checkContact(Contact contact, AuditAction action) {
        if (!canAccessOwnedBy(contact.getCompany().getOwner().getId())) {
            throw new ResourceAccessDeniedException("Contacto no encontrado.", action, AuditResourceType.CONTACT, contact.getId());
        }
    }

    public void checkOpportunity(Opportunity opportunity) {
        if (!canAccessOwnedBy(opportunity.getOwner().getId())) {
            throw new NotFoundException("Oportunidad no encontrada.");
        }
    }

    public void checkActivity(Activity activity) {
        if (!canAccessOwnedBy(activity.getOwner().getId())) {
            throw new NotFoundException("Actividad no encontrada.");
        }
    }

    public void checkQuote(Quote quote, AuditAction action) {
        if (!canAccessOwnedBy(quote.getOpportunity().getOwner().getId())) {
            throw new ResourceAccessDeniedException("Cotización no encontrada.", action, AuditResourceType.QUOTE, quote.getId());
        }
    }
}
