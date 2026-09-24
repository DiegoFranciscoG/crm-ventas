package ec.dfgranda.crm.service;

import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.AuditResourceType;
import ec.dfgranda.crm.domain.Company;
import ec.dfgranda.crm.domain.Contact;
import ec.dfgranda.crm.domain.ProcessingPurpose;
import ec.dfgranda.crm.dto.ConsentRequest;
import ec.dfgranda.crm.dto.ConsentResponse;
import ec.dfgranda.crm.dto.ContactCreateRequest;
import ec.dfgranda.crm.dto.ContactResponse;
import ec.dfgranda.crm.dto.ContactSummary;
import ec.dfgranda.crm.dto.ContactUpdateRequest;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.exception.BusinessRuleException;
import ec.dfgranda.crm.exception.NotFoundException;
import ec.dfgranda.crm.mapper.ContactMapper;
import ec.dfgranda.crm.repository.ContactConsentRepository;
import ec.dfgranda.crm.repository.ContactRepository;
import ec.dfgranda.crm.repository.Specs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Set;

/** Contactos (titulares de datos personales). Toda lectura o escritura de sus datos queda en el log de accesos. */
@Service
public class ContactService {

    private final ContactRepository contacts;
    private final ContactConsentRepository consentRepository;
    private final CompanyService companyService;
    private final ConsentService consentService;
    private final AccessPolicy accessPolicy;
    private final AccessAuditService audit;
    private final Clock clock;

    public ContactService(ContactRepository contacts, ContactConsentRepository consentRepository,
                          CompanyService companyService, ConsentService consentService, AccessPolicy accessPolicy,
                          AccessAuditService audit, Clock clock) {
        this.contacts = contacts;
        this.consentRepository = consentRepository;
        this.companyService = companyService;
        this.consentService = consentService;
        this.accessPolicy = accessPolicy;
        this.audit = audit;
        this.clock = clock;
    }

    /** Listado: se registra la consulta (sin datos personales en el detalle, solo el conteo). */
    @Transactional
    public PageResponse<ContactSummary> list(Long companyId, String search, int page, int size) {
        if (companyId != null) {
            companyService.load(companyId, AuditAction.LIST);
        }
        Page<Contact> result = contacts.findAll(
                Specs.all(Specs.contactOwnedBy(accessPolicy.ownerScope()), Specs.contactOfCompany(companyId),
                        Specs.contactMatches(search)),
                PageRequest.of(page, size, Sort.by("lastName", "firstName")));
        Set<Long> allowed = consentService.contactsWithActive(
                result.map(Contact::getId).getContent(), ProcessingPurpose.COMMERCIAL_CONTACT);
        audit.record(AuditAction.LIST, AuditResourceType.CONTACT, null,
                "results=" + result.getNumberOfElements() + (companyId == null ? "" : " companyId=" + companyId));
        return PageResponse.of(result, contact -> ContactMapper.toSummary(contact, allowed.contains(contact.getId())));
    }

    @Transactional
    public ContactResponse get(Long id) {
        Contact contact = load(id, AuditAction.VIEW);
        audit.record(AuditAction.VIEW, AuditResourceType.CONTACT, id, null);
        return toResponse(contact);
    }

    /** Alta con consentimiento inicial obligatorio para contacto comercial (el silencio no es consentimiento). */
    @Transactional
    public ContactResponse create(ContactCreateRequest request) {
        if (!ProcessingPurpose.COMMERCIAL_CONTACT.equals(request.initialConsent().purposeCode())) {
            throw new BusinessRuleException("El consentimiento inicial debe ser para contacto comercial.");
        }
        Company company = companyService.load(request.companyId(), AuditAction.CREATE);
        Contact contact = new Contact(company, request.firstName().trim(), request.lastName().trim(), request.dataSource());
        contact.setEmail(CompanyService.blankToNull(request.email()));
        contact.setPhone(CompanyService.blankToNull(request.phone()));
        contact.setJobTitle(CompanyService.blankToNull(request.jobTitle()));
        contact.setNationalId(request.nationalId());
        contact = contacts.save(contact);
        audit.record(AuditAction.CREATE, AuditResourceType.CONTACT, contact.getId(), null);
        consentService.grant(contact, request.initialConsent());
        return toResponse(contact);
    }

    /** Rectificación y actualización (LOPDP Art. 14). */
    @Transactional
    public ContactResponse update(Long id, ContactUpdateRequest request) {
        Contact contact = load(id, AuditAction.UPDATE);
        contact.setFirstName(request.firstName().trim());
        contact.setLastName(request.lastName().trim());
        contact.setEmail(CompanyService.blankToNull(request.email()));
        contact.setPhone(CompanyService.blankToNull(request.phone()));
        contact.setJobTitle(CompanyService.blankToNull(request.jobTitle()));
        contact.setDataSource(request.dataSource());
        if (request.nationalId() != null) {
            contact.setNationalId(request.nationalId());
        }
        audit.record(AuditAction.UPDATE, AuditResourceType.CONTACT, id, null);
        return toResponse(contact);
    }

    @Transactional
    public ConsentResponse grantConsent(Long contactId, ConsentRequest request) {
        return consentService.grant(load(contactId, AuditAction.CONSENT_GRANT), request);
    }

    @Transactional
    public ConsentResponse revokeConsent(Long contactId, Long consentId, String reason) {
        return consentService.revoke(load(contactId, AuditAction.CONSENT_REVOKE), consentId, reason);
    }

    /** Carga un contacto verificando la visibilidad; un acceso ajeno queda registrado como denegado. */
    @Transactional(readOnly = true)
    public Contact load(Long id, AuditAction intendedAction) {
        Contact contact = contacts.findById(id).orElseThrow(() -> new NotFoundException("Contacto no encontrado."));
        accessPolicy.checkContact(contact, intendedAction);
        return contact;
    }

    private ContactResponse toResponse(Contact contact) {
        return ContactMapper.toResponse(contact, consentRepository.findByContactIdOrderByGrantedAtDesc(contact.getId()),
                clock.instant());
    }
}
