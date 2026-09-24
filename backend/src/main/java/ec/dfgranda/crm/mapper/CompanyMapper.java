package ec.dfgranda.crm.mapper;

import ec.dfgranda.crm.domain.Company;
import ec.dfgranda.crm.dto.CompanyResponse;
import ec.dfgranda.crm.dto.CompanySummary;

public final class CompanyMapper {

    private CompanyMapper() {
    }

    /** En listas, el RUC de persona natural se enmascara porque contiene la cédula del titular. */
    public static CompanySummary toSummary(Company company) {
        String ruc = company.holdsPersonalData() ? Masking.maskIdentification(company.getRuc()) : company.getRuc();
        return new CompanySummary(company.getId(), ruc, company.getTaxpayerType(), company.getLegalName(),
                company.getTradeName(), company.getCity(), company.getProvince().getName(),
                UserMapper.toSummary(company.getOwner()));
    }

    public static CompanyResponse toResponse(Company company) {
        return new CompanyResponse(company.getId(), company.getRuc(), company.getTaxpayerType(),
                company.isRucChecksumVerified(), company.getLegalName(), company.getTradeName(),
                company.getIndustry() == null ? null : company.getIndustry().getCode(),
                company.getIndustry() == null ? null : company.getIndustry().getName(),
                company.getProvince().getCode(), company.getProvince().getName(), company.getCity(),
                company.getAddress(), company.getPhone(), company.getEmail(), company.getWebsite(),
                UserMapper.toSummary(company.getOwner()), company.getCreatedAt(), company.getUpdatedAt());
    }
}
