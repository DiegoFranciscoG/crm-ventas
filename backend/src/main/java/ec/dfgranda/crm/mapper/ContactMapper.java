package ec.dfgranda.crm.mapper;

import ec.dfgranda.crm.domain.Contact;
import ec.dfgranda.crm.domain.ContactConsent;
import ec.dfgranda.crm.dto.ConsentResponse;
import ec.dfgranda.crm.dto.ContactResponse;
import ec.dfgranda.crm.dto.ContactSummary;

import java.time.Instant;
import java.util.List;

public final class ContactMapper {

    private ContactMapper() {
    }

    public static ContactSummary toSummary(Contact contact, boolean commercialContactAllowed) {
        return new ContactSummary(contact.getId(), contact.getCompany().getId(), contact.getCompany().getLegalName(),
                contact.fullName(), contact.getEmail(), contact.getPhone(), contact.getJobTitle(),
                commercialContactAllowed);
    }

    public static ContactResponse toResponse(Contact contact, List<ContactConsent> consents, Instant now) {
        return new ContactResponse(contact.getId(), contact.getCompany().getId(), contact.getCompany().getLegalName(),
                contact.getFirstName(), contact.getLastName(), contact.getEmail(), contact.getPhone(),
                contact.getJobTitle(), Masking.maskIdentification(contact.getNationalId()), contact.getDataSource(),
                consents.stream().map(consent -> toResponse(consent, now)).toList(),
                contact.getCreatedAt(), contact.getUpdatedAt());
    }

    public static ConsentResponse toResponse(ContactConsent consent, Instant now) {
        return new ConsentResponse(consent.getId(), consent.getPurpose().getCode(), consent.getPurpose().getName(),
                consent.getLegalBasis(), consent.getChannel(), consent.getEvidence(),
                consent.getPrivacyNoticeVersion(), consent.getGrantedAt(), consent.getExpiresAt(),
                consent.getRevokedAt(), consent.getRevocationReason(), consent.isActiveAt(now),
                consent.getRecordedBy().getFullName());
    }
}
