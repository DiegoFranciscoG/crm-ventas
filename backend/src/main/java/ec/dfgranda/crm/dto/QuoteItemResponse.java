package ec.dfgranda.crm.dto;

import java.math.BigDecimal;

public record QuoteItemResponse(int lineNumber, Long productId, String sku, String description, BigDecimal quantity,
                                BigDecimal unitPrice, BigDecimal discountPercent, String vatRateCode,
                                BigDecimal vatPercentage, BigDecimal lineSubtotal, BigDecimal lineDiscount,
                                BigDecimal lineTax, BigDecimal lineTotal) {
}
