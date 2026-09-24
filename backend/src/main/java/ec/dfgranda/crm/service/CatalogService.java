package ec.dfgranda.crm.service;

import ec.dfgranda.crm.config.PrivacyProperties;
import ec.dfgranda.crm.domain.Role;
import ec.dfgranda.crm.domain.VatRate;
import ec.dfgranda.crm.dto.CatalogResponse;
import ec.dfgranda.crm.dto.CodeName;
import ec.dfgranda.crm.mapper.CatalogMapper;
import ec.dfgranda.crm.repository.IndustryRepository;
import ec.dfgranda.crm.repository.PipelineStageRepository;
import ec.dfgranda.crm.repository.ProcessingPurposeRepository;
import ec.dfgranda.crm.repository.ProvinceRepository;
import ec.dfgranda.crm.repository.VatRateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Catálogos oficiales para formularios y validación en el frontend. */
@Service
public class CatalogService {

    private static final Map<Role, String> ROLE_NAMES = Map.of(
            Role.ADMIN, "Administrador", Role.MANAGER, "Gerente comercial", Role.SALES_REP, "Vendedor");

    private final ProvinceRepository provinces;
    private final IndustryRepository industries;
    private final VatRateRepository vatRates;
    private final PipelineStageRepository stages;
    private final ProcessingPurposeRepository purposes;
    private final PrivacyProperties privacy;

    public CatalogService(ProvinceRepository provinces, IndustryRepository industries, VatRateRepository vatRates,
                          PipelineStageRepository stages, ProcessingPurposeRepository purposes, PrivacyProperties privacy) {
        this.provinces = provinces;
        this.industries = industries;
        this.vatRates = vatRates;
        this.stages = stages;
        this.purposes = purposes;
        this.privacy = privacy;
    }

    @Transactional(readOnly = true)
    public CatalogResponse all() {
        return new CatalogResponse(
                provinces.findAll(Sort.by("code")).stream().map(CatalogMapper::toCodeName).toList(),
                industries.findAll(Sort.by("code")).stream().map(CatalogMapper::toCodeName).toList(),
                vatRates.findAll(Sort.by("code")).stream().filter(VatRate::isActive).map(CatalogMapper::toResponse).toList(),
                stages.findAllByOrderByPositionAsc().stream().map(CatalogMapper::toResponse).toList(),
                purposes.findAll(Sort.by("code")).stream().map(CatalogMapper::toResponse).toList(),
                roles(),
                privacy.privacyNoticeVersion());
    }

    private static List<CodeName> roles() {
        return Arrays.stream(Role.values()).map(role -> new CodeName(role.name(), ROLE_NAMES.get(role))).toList();
    }
}
