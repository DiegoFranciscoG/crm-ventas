package ec.dfgranda.crm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Ítem de cotización. Con {@code productId}, la descripción, el precio y la tarifa salen del catálogo si no se
 * envían; sin producto (ítem libre) son obligatorios. Los totales siempre los calcula el servidor.
 */
public record QuoteItemRequest(
        Long productId,
        @Size(max = 500) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal quantity,
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal unitPrice,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal discountPercent,
        @Size(max = 2) String vatRateCode) {
}
