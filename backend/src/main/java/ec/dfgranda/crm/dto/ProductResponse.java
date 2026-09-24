package ec.dfgranda.crm.dto;

import java.math.BigDecimal;

public record ProductResponse(Long id, String sku, String name, String description, BigDecimal unitPrice,
                              String vatRateCode, BigDecimal vatPercentage, boolean active) {
}
