package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.ContactSource;
import ec.dfgranda.crm.validation.ValidCedula;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de contacto. Exige el consentimiento inicial de contacto comercial: el silencio no es consentimiento
 * (Reglamento LOPDP Art. 5), así que no se puede registrar a una persona sin una base demostrable.
 */
public record ContactCreateRequest(
        @NotNull Long companyId,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "^[0-9+() -]{7,20}$", message = "Teléfono inválido") String phone,
        @Size(max = 100) String jobTitle,
        @ValidCedula String nationalId,
        @NotNull ContactSource dataSource,
        @NotNull @Valid ConsentRequest initialConsent) {
}
