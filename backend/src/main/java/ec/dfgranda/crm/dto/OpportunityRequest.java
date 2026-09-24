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

/**
 * Alta de oportunidad. {@code stageCode} por defecto es PROSPECTING; {@code probability} por defecto es la de la
 * etapa. {@code ownerId} solo lo aplica un gerente.
 */
public record OpportunityRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull Long companyId,
        Long primaryContactId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull LocalDate expectedCloseDate,
        @Size(max = 30) String stageCode,
        @Min(0) @Max(100) Integer probability,
        @Size(max = 2000) String description,
        Long ownerId) {
}
