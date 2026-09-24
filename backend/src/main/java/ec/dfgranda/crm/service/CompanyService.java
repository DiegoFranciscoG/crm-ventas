package ec.dfgranda.crm.service;

import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.AuditResourceType;
import ec.dfgranda.crm.domain.Company;
import ec.dfgranda.crm.domain.Industry;
import ec.dfgranda.crm.dto.CompanyRequest;
import ec.dfgranda.crm.dto.CompanyResponse;
import ec.dfgranda.crm.dto.CompanySummary;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.exception.BusinessRuleException;
import ec.dfgranda.crm.exception.ConflictException;
import ec.dfgranda.crm.exception.NotFoundException;
import ec.dfgranda.crm.mapper.CompanyMapper;
import ec.dfgranda.crm.repository.CompanyRepository;
import ec.dfgranda.crm.repository.ContactRepository;
import ec.dfgranda.crm.repository.IndustryRepository;
import ec.dfgranda.crm.repository.OpportunityRepository;
import ec.dfgranda.crm.repository.ProvinceRepository;
import ec.dfgranda.crm.repository.Specs;
import ec.dfgranda.crm.validation.EcuadorianIdValidator;
import ec.dfgranda.crm.validation.IdentificationResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Empresas cliente. El RUC se valida con las reglas del SRI y el tipo de contribuyente se deriva de él. */
@Service
public class CompanyService {

    private final CompanyRepository companies;
    private final ContactRepository contacts;
    private final OpportunityRepository opportunities;
    private final ProvinceRepository provinces;
    private final IndustryRepository industries;
    private final UserService userService;
    private final AccessPolicy accessPolicy;
    private final AccessAuditService audit;

    public CompanyService(CompanyRepository companies, ContactRepository contacts, OpportunityRepository opportunities,
                          ProvinceRepository provinces, IndustryRepository industries, UserService userService,
                          AccessPolicy accessPolicy, AccessAuditService audit) {
        this.companies = companies;
        this.contacts = contacts;
        this.opportunities = opportunities;
        this.provinces = provinces;
        this.industries = industries;
        this.userService = userService;
        this.accessPolicy = accessPolicy;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanySummary> list(String search, int page, int size) {
        return PageResponse.of(companies.findAll(
                Specs.all(Specs.companyOwnedBy(accessPolicy.ownerScope()), Specs.companyMatches(search)),
                PageRequest.of(page, size, Sort.by("legalName"))), CompanyMapper::toSummary);
    }

    /** El RUC de persona natural es dato personal: su consulta queda en el log de accesos. */
    @Transactional
    public CompanyResponse get(Long id) {
        Company company = load(id, AuditAction.VIEW);
        if (company.holdsPersonalData()) {
            audit.record(AuditAction.VIEW, AuditResourceType.COMPANY, company.getId(), null);
        }
        return CompanyMapper.toResponse(company);
    }

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        IdentificationResult ruc = EcuadorianIdValidator.validateRuc(request.ruc());
        if (companies.existsByRuc(request.ruc())) {
            throw new ConflictException("Ya existe una empresa registrada con ese RUC.");
        }
        Company company = new Company(request.ruc(), ruc.taxpayerType(), ruc.checksumVerified(),
                userService.resolveOwner(request.ownerId()));
        apply(company, request);
        company = companies.save(company);
        if (company.holdsPersonalData()) {
            audit.record(AuditAction.CREATE, AuditResourceType.COMPANY, company.getId(), null);
        }
        return CompanyMapper.toResponse(company);
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = load(id, AuditAction.UPDATE);
        if (!company.getRuc().equals(request.ruc())) {
            if (companies.existsByRucAndIdNot(request.ruc(), id)) {
                throw new ConflictException("Ya existe una empresa registrada con ese RUC.");
            }
            IdentificationResult ruc = EcuadorianIdValidator.validateRuc(request.ruc());
            company.changeRuc(request.ruc(), ruc.taxpayerType(), ruc.checksumVerified());
        }
        apply(company, request);
        if (request.ownerId() != null && accessPolicy.user().hasGlobalVisibility()) {
            company.setOwner(userService.resolveOwner(request.ownerId()));
        }
        if (company.holdsPersonalData()) {
            audit.record(AuditAction.UPDATE, AuditResourceType.COMPANY, company.getId(), null);
        }
        return CompanyMapper.toResponse(company);
    }

    @Transactional
    public void delete(Long id) {
        Company company = load(id, AuditAction.ERASE);
        if (contacts.existsByCompanyId(id) || opportunities.existsByCompanyId(id)) {
            throw new ConflictException("La empresa tiene contactos u oportunidades; elimínelos o reasígnelos primero.");
        }
        companies.delete(company);
    }

    /** Carga una empresa verificando que el usuario pueda verla (BOLA). */
    @Transactional(readOnly = true)
    public Company load(Long id, AuditAction intendedAction) {
        Company company = companies.findById(id).orElseThrow(() -> new NotFoundException("Empresa no encontrada."));
        accessPolicy.checkCompany(company, intendedAction);
        return company;
    }

    private void apply(Company company, CompanyRequest request) {
        company.setLegalName(request.legalName().trim());
        company.setTradeName(blankToNull(request.tradeName()));
        company.setProvince(provinces.findById(request.provinceCode())
                .orElseThrow(() -> new BusinessRuleException("La provincia no existe en el catálogo del INEC.")));
        Industry industry = request.industryCode() == null ? null : industries.findById(request.industryCode())
                .orElseThrow(() -> new BusinessRuleException("La sección CIIU no existe."));
        company.setIndustry(industry);
        company.setCity(blankToNull(request.city()));
        company.setAddress(blankToNull(request.address()));
        company.setPhone(blankToNull(request.phone()));
        company.setEmail(blankToNull(request.email()));
        company.setWebsite(blankToNull(request.website()));
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
