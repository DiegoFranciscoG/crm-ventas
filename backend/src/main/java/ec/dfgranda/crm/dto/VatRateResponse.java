package ec.dfgranda.crm.dto;

import java.math.BigDecimal;

public record VatRateResponse(String code, String description, BigDecimal percentage) {
}
