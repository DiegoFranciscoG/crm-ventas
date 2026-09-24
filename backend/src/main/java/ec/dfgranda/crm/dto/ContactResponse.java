package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.ContactSource;

import java.time.Instant;
import java.util.List;

/** La cédula nunca se devuelve completa en la API operativa: solo enmascarada (minimización). */
public record ContactResponse(Long id, Long companyId, String companyName, String firstName, String lastName,
                              String email, String phone, String jobTitle, String nationalIdMasked,
                              ContactSource dataSource, List<ConsentResponse> consents, Instant createdAt,
                              Instant updatedAt) {
}
