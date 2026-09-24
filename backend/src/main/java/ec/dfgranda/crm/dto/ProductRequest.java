package ec.dfgranda.crm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9-]{2,40}$", message = "SKU: letras, números y guiones (2 a 40)") String sku,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal unitPrice,
        @NotBlank @Size(max = 2) String vatRateCode,
        boolean active) {
}
