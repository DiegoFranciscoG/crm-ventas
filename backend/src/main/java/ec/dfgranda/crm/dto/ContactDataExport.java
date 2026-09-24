package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.ContactSource;

import java.time.Instant;
import java.util.List;

/**
 * Exportación de los datos del titular en formato estructurado y de lectura mecánica (LOPDP Arts. 13 y 17).
 * Incluye la información del Art. 12: responsable, finalidades, bases legales y plazos de conservación.
 */
public record ContactDataExport(
        String format,
        Instant generatedAt,
        DataController controller,
        PersonalData personalData,
        List<ConsentResponse> consents,
        List<PurposeResponse> purposes,
        List<ActivityEntry> activities,
        List<QuoteEntry> quotes,
        List<String> rights) {

    public record DataController(String legalName, String ruc, String address, String email, String phone) {
    }

    public record PersonalData(Long contactId, String firstName, String lastName, String email, String phone,
                               String jobTitle, String nationalId, ContactSource dataSource, String companyLegalName,
                               Instant createdAt, Instant updatedAt) {
    }

    public record ActivityEntry(String type, String subject, Instant dueAt, Instant completedAt) {
    }

    public record QuoteEntry(String number, String status, String issueDate) {
    }
}
