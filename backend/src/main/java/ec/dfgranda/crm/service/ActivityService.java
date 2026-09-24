package ec.dfgranda.crm.service;

import ec.dfgranda.crm.domain.Activity;
import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.Company;
import ec.dfgranda.crm.domain.Contact;
import ec.dfgranda.crm.domain.Opportunity;
import ec.dfgranda.crm.domain.ProcessingPurpose;
import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.dto.ActivityRequest;
import ec.dfgranda.crm.dto.ActivityResponse;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.exception.BusinessRuleException;
import ec.dfgranda.crm.exception.NotFoundException;
import ec.dfgranda.crm.mapper.ActivityMapper;
import ec.dfgranda.crm.repository.ActivityRepository;
import ec.dfgranda.crm.repository.Specs;
import ec.dfgranda.crm.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Agenda de actividades con recordatorios. Contactar a una persona exige su consentimiento vigente. */
@Service
public class ActivityService {

    private final ActivityRepository activities;
    private final UserRepository users;
    private final CompanyService companyService;
    private final ContactService contactService;
    private final OpportunityService opportunityService;
    private final ConsentService consentService;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public ActivityService(ActivityRepository activities, UserRepository users, CompanyService companyService,
                           ContactService contactService, OpportunityService opportunityService,
                           ConsentService consentService, AccessPolicy accessPolicy, Clock clock) {
        this.activities = activities;
        this.users = users;
        this.companyService = companyService;
        this.contactService = contactService;
        this.opportunityService = opportunityService;
        this.consentService = consentService;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> list(Long opportunityId, Long companyId, Long contactId, Boolean completed,
                                               Instant from, Instant to, int page, int size) {
        Instant now = clock.instant();
        return PageResponse.of(activities.findAll(
                Specs.all(Specs.activityOwnedBy(accessPolicy.ownerScope()), Specs.activityOfOpportunity(opportunityId),
                        Specs.activityOfCompany(companyId), Specs.activityOfContact(contactId),
                        Specs.activityCompleted(completed), Specs.activityDueBetween(from, to)),
                PageRequest.of(page, size, Sort.by(Sort.Order.asc("dueAt").nullsLast(), Sort.Order.desc("id")))),
                activity -> ActivityMapper.toResponse(activity, now));
    }

    @Transactional
    public ActivityResponse create(ActivityRequest request) {
        User owner = users.getReferenceById(accessPolicy.user().id());
        Activity activity = new Activity(request.type(), request.subject().trim(), owner);
        apply(activity, request);
        return ActivityMapper.toResponse(activities.save(activity), clock.instant());
    }

    @Transactional
    public ActivityResponse update(Long id, ActivityRequest request) {
        Activity activity = load(id);
        activity.setType(request.type());
        activity.setSubject(request.subject().trim());
        apply(activity, request);
        return ActivityMapper.toResponse(activity, clock.instant());
    }

    @Transactional
    public ActivityResponse complete(Long id, boolean completed) {
        Activity activity = load(id);
        if (completed) {
            activity.complete(clock.instant());
        } else {
            activity.reopen();
        }
        return ActivityMapper.toResponse(activity, clock.instant());
    }

    @Transactional
    public void delete(Long id) {
        activities.delete(load(id));
    }

    private Activity load(Long id) {
        Activity activity = activities.findById(id).orElseThrow(() -> new NotFoundException("Actividad no encontrada."));
        accessPolicy.checkActivity(activity);
        return activity;
    }

    private void apply(Activity activity, ActivityRequest request) {
        Opportunity opportunity = request.opportunityId() == null ? null : opportunityService.load(request.opportunityId());
        Company company = opportunity != null ? opportunity.getCompany()
                : request.companyId() == null ? null : companyService.load(request.companyId(), AuditAction.VIEW);
        if (company == null) {
            throw new BusinessRuleException("La actividad debe estar asociada a una empresa o a una oportunidad.");
        }
        Contact contact = null;
        if (request.contactId() != null) {
            contact = contactService.load(request.contactId(), AuditAction.VIEW);
            if (!contact.getCompany().getId().equals(company.getId())) {
                throw new BusinessRuleException("El contacto debe pertenecer a la empresa de la actividad.");
            }
            activity.setType(request.type());
            if (activity.involvesDirectContact()) {
                consentService.requireActive(contact, ProcessingPurpose.COMMERCIAL_CONTACT, "agendar contacto directo");
            }
        }
        activity.setOpportunity(opportunity);
        activity.setCompany(company);
        activity.setContact(contact);
        activity.setNotes(CompanyService.blankToNull(request.notes()));
        activity.schedule(request.dueAt(), request.remindAt());
    }
}
