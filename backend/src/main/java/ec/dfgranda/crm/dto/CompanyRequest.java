package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.validation.ValidRuc;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code ownerId} solo lo usa un gerente para asignar la cuenta; si lo envía un vendedor se ignora. */
public record CompanyRequest(
        @NotBlank @ValidRuc String ruc,
        @NotBlank @Size(max = 300) String legalName,
        @Size(max = 300) String tradeName,
        @Pattern(regexp = "^[A-U]$", message = "Sección CIIU inválida") String industryCode,
        @NotBlank @Pattern(regexp = "^[0-9]{2}$", message = "Código de provincia inválido") String provinceCode,
        @Size(max = 100) String city,
        @Size(max = 300) String address,
        @Pattern(regexp = "^[0-9+() -]{7,20}$", message = "Teléfono inválido") String phone,
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "^https://\\S{3,190}$", message = "El sitio web debe empezar con https://") String website,
        Long ownerId) {
}
