package ec.dfgranda.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IdentificationRequest(@NotNull Type type, @NotBlank @Size(max = 13) String number) {

    public enum Type { RUC, CEDULA }
}
