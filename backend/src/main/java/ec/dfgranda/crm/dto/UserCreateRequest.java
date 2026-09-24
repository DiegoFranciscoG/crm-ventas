package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Contraseña de 12 a 72 caracteres: bcrypt ignora lo que pasa de 72 bytes (investigación #23). */
public record UserCreateRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotNull Role role) {
}
