package ec.dfgranda.crm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code version} activa el bloqueo optimista: si otro usuario editó antes, se responde 409. */
public record OpportunityUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        Long primaryContactId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull LocalDate expectedCloseDate,
        @Min(0) @Max(100) Integer probability,
        @Size(max = 2000) String description,
        Long ownerId,
        @NotNull Long version) {
}
