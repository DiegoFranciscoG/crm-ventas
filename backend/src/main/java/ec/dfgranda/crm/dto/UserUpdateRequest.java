package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code newPassword} es opcional: si viene, reemplaza la contraseña. */
public record UserUpdateRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotNull Role role,
        boolean active,
        @Size(min = 12, max = 72) String newPassword) {
}
