package ec.dfgranda.crm.service;

import ec.dfgranda.crm.config.PrivacyProperties;
import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.AuditResourceType;
import ec.dfgranda.crm.domain.Contact;
import ec.dfgranda.crm.domain.ContactConsent;
import ec.dfgranda.crm.domain.ProcessingPurpose;
import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.dto.ConsentRequest;
import ec.dfgranda.crm.dto.ConsentResponse;
import ec.dfgranda.crm.exception.BusinessRuleException;
import ec.dfgranda.crm.exception.ConflictException;
import ec.dfgranda.crm.exception.NotFoundException;
import ec.dfgranda.crm.mapper.ContactMapper;
import ec.dfgranda.crm.repository.ContactConsentRepository;
import ec.dfgranda.crm.repository.ProcessingPurposeRepository;
import ec.dfgranda.crm.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Registro de consentimientos por finalidad (LOPDP Art. 8; Reglamento Arts. 5-6) y verificación antes de tratar
 * datos: sin consentimiento vigente para una finalidad, esa operación se bloquea.
 */
@Service
public class ConsentService {

    private final ContactConsentRepository consents;
    private final ProcessingPurposeRepository purposes;
    private final UserRepository users;
    private final AccessAuditService audit;
    private final AccessPolicy accessPolicy;
    private final PrivacyProperties privacy;
    private final Clock clock;

    public ConsentService(ContactConsentRepository consents, ProcessingPurposeRepository purposes, UserRepository users,
                          AccessAuditService audit, AccessPolicy accessPolicy, PrivacyProperties privacy, Clock clock) {
        this.consents = consents;
        this.purposes = purposes;
        this.users = users;
        this.audit = audit;
        this.accessPolicy = accessPolicy;
        this.privacy = privacy;
        this.clock = clock;
    }

    /** Registra un consentimiento nuevo. Si ya hay uno vigente para la finalidad, se responde 409. */
    @Transactional
    public ConsentResponse grant(Contact contact, ConsentRequest request) {
        ProcessingPurpose purpose = purposes.findById(request.purposeCode())
                .orElseThrow(() -> new BusinessRuleException("Finalidad de tratamiento inexistente."));
        consents.findFirstByContactIdAndPurposeCodeAndRevokedAtIsNull(contact.getId(), purpose.getCode())
                .ifPresent(existing -> {
                    if (existing.isActiveAt(clock.instant())) {
                        throw new ConflictException("Ya existe un consentimiento vigente para esa finalidad.");
                    }
                    // Vencido por plazo de conservación: se cierra para poder registrar uno nuevo.
                    existing.revoke(clock.instant(), "Renovado: el anterior venció por plazo de conservación.");
                });
        consents.flush();
        Instant now = clock.instant();
        Instant expiresAt = now.atOffset(ZoneOffset.UTC).plusMonths(purpose.getRetentionMonths()).toInstant();
        User recordedBy = users.getReferenceById(accessPolicy.user().id());
        ContactConsent consent = consents.save(new ContactConsent(contact, purpose, request.channel(),
                request.evidence().trim(), privacy.privacyNoticeVersion(), now, expiresAt, recordedBy));
        audit.record(AuditAction.CONSENT_GRANT, AuditResourceType.CONTACT, contact.getId(), "purpose=" + purpose.getCode());
        return ContactMapper.toResponse(consent, now);
    }

    /** Revoca sin exigir justificación y sin borrar el registro (la revocatoria no es retroactiva). */
    @Transactional
    public ConsentResponse revoke(Contact contact, Long consentId, String reason) {
        ContactConsent consent = consents.findById(consentId)
                .filter(c -> c.getContact().getId().equals(contact.getId()))
                .orElseThrow(() -> new NotFoundException("Consentimiento no encontrado."));
        Instant now = clock.instant();
        consent.revoke(now, reason == null || reason.isBlank() ? null : reason.trim());
        audit.record(AuditAction.CONSENT_REVOKE, AuditResourceType.CONTACT, contact.getId(),
                "purpose=" + consent.getPurpose().getCode());
        return ContactMapper.toResponse(consent, now);
    }

    @Transactional(readOnly = true)
    public boolean hasActive(Long contactId, String purposeCode) {
        return consents.existsActive(contactId, purposeCode, clock.instant());
    }

    @Transactional(readOnly = true)
    public Set<Long> contactsWithActive(Collection<Long> contactIds, String purposeCode) {
        if (contactIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(consents.findContactIdsWithActive(contactIds, purposeCode, clock.instant()));
    }

    /** Bloquea el tratamiento si no hay consentimiento vigente para la finalidad (Reglamento Art. 6). */
    public void requireActive(Contact contact, String purposeCode, String operation) {
        if (!hasActive(contact.getId(), purposeCode)) {
            String purposeName = purposes.findById(purposeCode).map(ProcessingPurpose::getName).orElse(purposeCode);
            throw new BusinessRuleException("No se puede " + operation + ": " + contact.fullName()
                    + " no tiene un consentimiento vigente para \"" + purposeName + "\".");
        }
    }
}
