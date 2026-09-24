package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.ContactSource;
import ec.dfgranda.crm.validation.ValidCedula;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Rectificación y actualización de datos (LOPDP Art. 14). Si {@code nationalId} es {@code null} se conserva la
 * cédula actual; si viene vacío ({@code ""}) se elimina.
 */
public record ContactUpdateRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "^[0-9+() -]{7,20}$", message = "Teléfono inválido") String phone,
        @Size(max = 100) String jobTitle,
        @ValidCedula String nationalId,
        @NotNull ContactSource dataSource) {
}
