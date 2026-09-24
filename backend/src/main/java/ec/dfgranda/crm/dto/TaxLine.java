package ec.dfgranda.crm.dto;

import java.math.BigDecimal;

/** Base imponible e IVA por tarifa, como en los comprobantes del SRI (subtotal 15 %, subtotal 0 %...). */
public record TaxLine(String vatRateCode, BigDecimal percentage, BigDecimal taxableBase, BigDecimal tax) {
}
