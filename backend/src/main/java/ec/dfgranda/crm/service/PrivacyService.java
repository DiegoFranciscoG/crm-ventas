package ec.dfgranda.crm.service;

import ec.dfgranda.crm.config.PrivacyProperties;
import ec.dfgranda.crm.config.SellerProperties;
import ec.dfgranda.crm.domain.Activity;
import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.AuditResourceType;
import ec.dfgranda.crm.domain.Contact;
import ec.dfgranda.crm.domain.DataSubjectRequest;
import ec.dfgranda.crm.domain.Opportunity;
import ec.dfgranda.crm.domain.Quote;
import ec.dfgranda.crm.domain.RequestType;
import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.dto.ContactDataExport;
import ec.dfgranda.crm.dto.DataSubjectRequestCreate;
import ec.dfgranda.crm.dto.DataSubjectRequestResponse;
import ec.dfgranda.crm.dto.ErasureResponse;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.mapper.CatalogMapper;
import ec.dfgranda.crm.mapper.ContactMapper;
import ec.dfgranda.crm.mapper.PrivacyMapper;
import ec.dfgranda.crm.repository.ActivityRepository;
import ec.dfgranda.crm.repository.ContactConsentRepository;
import ec.dfgranda.crm.repository.ContactRepository;
import ec.dfgranda.crm.repository.DataSubjectRequestRepository;
import ec.dfgranda.crm.repository.OpportunityRepository;
import ec.dfgranda.crm.repository.ProcessingPurposeRepository;
import ec.dfgranda.crm.repository.QuoteRepository;
import ec.dfgranda.crm.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.List;

/**
 * Derechos del titular (LOPDP Arts. 13-17) con registro obligatorio de cada solicitud (Reglamento Art. 15).
 * Solo gerente y administrador ejecutan estas operaciones (lo exige también la configuración de seguridad).
 */
@Service
public class PrivacyService {

    static final String EXPORT_FORMAT = "crm-ventas/contact-export@1";
    private static final List<String> RIGHTS = List.of(
            "Acceso (LOPDP Art. 13)", "Rectificación y actualización (Art. 14)", "Eliminación (Art. 15)",
            "Oposición (Art. 16)", "Portabilidad (Art. 17)", "Suspensión del tratamiento (Art. 19)",
            "Revocar el consentimiento en cualquier momento, sin efecto retroactivo (Art. 8)",
            "Presentar un reclamo ante la Superintendencia de Protección de Datos Personales");

    private final ContactService contactService;
    private final ContactRepository contacts;
    private final ContactConsentRepository consents;
    private final ActivityRepository activities;
    private final QuoteRepository quotes;
    private final OpportunityRepository opportunities;
    private final ProcessingPurposeRepository purposes;
    private final DataSubjectRequestRepository requests;
    private final UserRepository users;
    private final AccessPolicy accessPolicy;
    private final AccessAuditService audit;
    private final PrivacyProperties privacy;
    private final SellerProperties seller;
    private final Clock clock;

    public PrivacyService(ContactService contactService, ContactRepository contacts, ContactConsentRepository consents,
                          ActivityRepository activities, QuoteRepository quotes, OpportunityRepository opportunities,
                          ProcessingPurposeRepository purposes, DataSubjectRequestRepository requests,
                          UserRepository users, AccessPolicy accessPolicy, AccessAuditService audit,
                          PrivacyProperties privacy, SellerProperties seller, Clock clock) {
        this.contactService = contactService;
        this.contacts = contacts;
        this.consents = consents;
        this.activities = activities;
        this.quotes = quotes;
        this.opportunities = opportunities;
        this.purposes = purposes;
        this.requests = requests;
        this.users = users;
        this.accessPolicy = accessPolicy;
        this.audit = audit;
        this.privacy = privacy;
        this.seller = seller;
        this.clock = clock;
    }

    /** Acceso y portabilidad: todos los datos del titular en JSON estructurado, con la información del Art. 12. */
    @Transactional
    public ContactDataExport export(Long contactId, DataSubjectRequestCreate request) {
        Contact contact = contactService.load(contactId, AuditAction.EXPORT);
        Instant now = clock.instant();
        ContactDataExport export = new ContactDataExport(
                EXPORT_FORMAT, now,
                new ContactDataExport.DataController(seller.legalName(), seller.ruc(), seller.address() + ", " + seller.city(),
                        seller.email(), seller.phone()),
                new ContactDataExport.PersonalData(contact.getId(), contact.getFirstName(), contact.getLastName(),
                        contact.getEmail(), contact.getPhone(), contact.getJobTitle(), contact.getNationalId(),
                        contact.getDataSource(), contact.getCompany().getLegalName(), contact.getCreatedAt(),
                        contact.getUpdatedAt()),
                consents.findByContactIdOrderByGrantedAtDesc(contactId).stream()
                        .map(consent -> ContactMapper.toResponse(consent, now)).toList(),
                purposes.findAll().stream().map(CatalogMapper::toResponse).toList(),
                activities.findByContactId(contactId).stream()
                        .map(a -> new ContactDataExport.ActivityEntry(a.getType().name(), a.getSubject(), a.getDueAt(), a.getCompletedAt()))
                        .toList(),
                quotes.findByContactId(contactId).stream()
                        .map(q -> new ContactDataExport.QuoteEntry(q.getNumber(), q.getStatus().name(), q.getIssueDate().toString()))
                        .toList(),
                RIGHTS);
        DataSubjectRequest record = register(contactId, RequestType.PORTABILITY, request, now);
        record.complete(now, resolution("Se generó la exportación JSON de los datos del titular.", request));
        audit.record(AuditAction.EXPORT, AuditResourceType.CONTACT, contactId, "requestId=" + record.getId());
        return export;
    }

    /**
     * Eliminación (LOPDP Art. 15): borra al contacto y sus consentimientos; en los registros comerciales que lo
     * referencian se desvincula y se anonimiza el texto de sus actividades. El registro de la solicitud y el log de
     * accesos solo conservan el ID, sin datos personales.
     */
    @Transactional
    public ErasureResponse erase(Long contactId, DataSubjectRequestCreate request) {
        Contact contact = contactService.load(contactId, AuditAction.ERASE);
        Instant now = clock.instant();

        List<Activity> linkedActivities = activities.findByContactId(contactId);
        linkedActivities.forEach(Activity::detachErasedContact);
        List<Quote> linkedQuotes = quotes.findByContactId(contactId);
        linkedQuotes.forEach(Quote::detachErasedContact);
        List<Opportunity> linkedOpportunities = opportunities.findByPrimaryContactId(contactId);
        linkedOpportunities.forEach(o -> o.setPrimaryContact(null));
        int deletedConsents = consents.deleteByContactId(contactId);
        contacts.delete(contact);
        contacts.flush();

        DataSubjectRequest record = register(contactId, RequestType.ERASURE, request, now);
        record.complete(now, resolution("Datos eliminados; " + linkedActivities.size() + " actividades anonimizadas, "
                + linkedQuotes.size() + " cotizaciones y " + linkedOpportunities.size() + " oportunidades desvinculadas.", request));
        audit.record(AuditAction.ERASE, AuditResourceType.CONTACT, contactId, "requestId=" + record.getId());
        return new ErasureResponse(PrivacyMapper.toResponse(record), linkedActivities.size(), linkedQuotes.size(),
                linkedOpportunities.size(), deletedConsents);
    }

    @Transactional(readOnly = true)
    public PageResponse<DataSubjectRequestResponse> listRequests(int page, int size) {
        return PageResponse.of(requests.findAllByOrderByReceivedAtDesc(PageRequest.of(page, size)), PrivacyMapper::toResponse);
    }

    private DataSubjectRequest register(Long contactId, RequestType type, DataSubjectRequestCreate request, Instant now) {
        User handledBy = users.getReferenceById(accessPolicy.user().id());
        Instant dueAt = now.plus(Duration.ofDays(privacy.requestDueDays()));
        return requests.save(new DataSubjectRequest(contactId, type, request.channel(), now, dueAt, handledBy));
    }

    private static String resolution(String base, DataSubjectRequestCreate request) {
        String notes = request.notes() == null || request.notes().isBlank() ? "" : " Nota: " + request.notes().trim();
        String text = base + notes;
        return text.length() > 500 ? text.substring(0, 500) : text;
    }
}
