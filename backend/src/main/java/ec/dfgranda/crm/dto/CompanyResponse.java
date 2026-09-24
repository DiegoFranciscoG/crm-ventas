package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.validation.TaxpayerType;

import java.time.Instant;

public record CompanyResponse(Long id, String ruc, TaxpayerType taxpayerType, boolean rucChecksumVerified,
                              String legalName, String tradeName, String industryCode, String industryName,
                              String provinceCode, String provinceName, String city, String address, String phone,
                              String email, String website, UserSummary owner, Instant createdAt, Instant updatedAt) {
}
