package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.validation.TaxpayerType;

/** Vista de lista: el RUC de personas naturales se enmascara porque contiene la cédula. */
public record CompanySummary(Long id, String ruc, TaxpayerType taxpayerType, String legalName, String tradeName,
                             String city, String provinceName, UserSummary owner) {
}
