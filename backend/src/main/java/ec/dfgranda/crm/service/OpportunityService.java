package ec.dfgranda.crm.service;

import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.Company;
import ec.dfgranda.crm.domain.Contact;
import ec.dfgranda.crm.domain.Opportunity;
import ec.dfgranda.crm.domain.OpportunityStageHistory;
import ec.dfgranda.crm.domain.PipelineStage;
import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.dto.BoardColumn;
import ec.dfgranda.crm.dto.OpportunityCard;
import ec.dfgranda.crm.dto.OpportunityRequest;
import ec.dfgranda.crm.dto.OpportunityResponse;
import ec.dfgranda.crm.dto.OpportunityUpdateRequest;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.dto.StageChangeRequest;
import ec.dfgranda.crm.dto.StageHistoryResponse;
import ec.dfgranda.crm.exception.BusinessRuleException;
import ec.dfgranda.crm.exception.ConflictException;
import ec.dfgranda.crm.exception.NotFoundException;
import ec.dfgranda.crm.mapper.CatalogMapper;
import ec.dfgranda.crm.mapper.OpportunityMapper;
import ec.dfgranda.crm.repository.OpportunityRepository;
import ec.dfgranda.crm.repository.OpportunityStageHistoryRepository;
import ec.dfgranda.crm.repository.PipelineStageRepository;
import ec.dfgranda.crm.repository.Specs;
import ec.dfgranda.crm.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Oportunidades y tablero kanban. Cada cambio de etapa aplica la probabilidad por defecto y deja historial. */
@Service
public class OpportunityService {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Guayaquil");
    private static final Duration RECENTLY_CLOSED = Duration.ofDays(30);
    private static final String DEFAULT_STAGE = "PROSPECTING";

    private final OpportunityRepository opportunities;
    private final OpportunityStageHistoryRepository history;
    private final PipelineStageRepository stages;
    private final UserRepository users;
    private final CompanyService companyService;
    private final ContactService contactService;
    private final UserService userService;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public OpportunityService(OpportunityRepository opportunities, OpportunityStageHistoryRepository history,
                              PipelineStageRepository stages, UserRepository users, CompanyService companyService,
                              ContactService contactService, UserService userService, AccessPolicy accessPolicy,
                              Clock clock) {
        this.opportunities = opportunities;
        this.history = history;
        this.stages = stages;
        this.users = users;
        this.companyService = companyService;
        this.contactService = contactService;
        this.userService = userService;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<OpportunityResponse> list(String stageCode, Long companyId, Long ownerId, String search,
                                                  int page, int size) {
        Long scope = accessPolicy.ownerScope();
        Long ownerFilter = scope != null ? scope : ownerId;
        return PageResponse.of(opportunities.findAll(
                Specs.all(Specs.opportunityOwnedBy(ownerFilter), Specs.opportunityOfCompany(companyId),
                        Specs.opportunityInStage(stageCode), Specs.opportunityMatches(search)),
                PageRequest.of(page, size, Sort.by("expectedCloseDate", "id"))), OpportunityMapper::toResponse);
    }

    /** Tablero: una columna por etapa con oportunidades abiertas y las cerradas en los últimos 30 días. */
    @Transactional(readOnly = true)
    public List<BoardColumn> board(Long ownerId) {
        Long scope = accessPolicy.ownerScope();
        Long ownerFilter = scope != null ? scope : ownerId;
        List<Opportunity> visible = opportunities.findAll(
                Specs.all(Specs.opportunityOwnedBy(ownerFilter), Specs.openOrClosedSince(clock.instant().minus(RECENTLY_CLOSED))),
                Sort.by("expectedCloseDate", "id"));
        Map<String, List<Opportunity>> byStage = visible.stream()
                .collect(Collectors.groupingBy(o -> o.getStage().getCode()));
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        List<BoardColumn> columns = new ArrayList<>();
        for (PipelineStage stage : stages.findAllByOrderByPositionAsc()) {
            List<Opportunity> inStage = byStage.getOrDefault(stage.getCode(), List.of());
            List<OpportunityCard> cards = inStage.stream().map(o -> OpportunityMapper.toCard(o, today)).toList();
            BigDecimal total = inStage.stream().map(Opportunity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal weighted = inStage.stream().map(Opportunity::weightedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            columns.add(new BoardColumn(CatalogMapper.toResponse(stage), cards.size(), total, weighted, cards));
        }
        return columns;
    }

    @Transactional(readOnly = true)
    public OpportunityResponse get(Long id) {
        return OpportunityMapper.toResponse(load(id));
    }

    @Transactional
    public OpportunityResponse create(OpportunityRequest request) {
        Company company = companyService.load(request.companyId(), AuditAction.CREATE);
        PipelineStage stage = stage(request.stageCode() == null ? DEFAULT_STAGE : request.stageCode());
        if (stage.isClosed()) {
            throw new BusinessRuleException("Una oportunidad nueva debe empezar en una etapa abierta.");
        }
        User owner = userService.resolveOwner(request.ownerId());
        Opportunity opportunity = new Opportunity(request.title().trim(), company, owner, stage, request.amount(),
                request.expectedCloseDate());
        opportunity.setPrimaryContact(contactOf(company, request.primaryContactId()));
        opportunity.setDescription(CompanyService.blankToNull(request.description()));
        if (request.probability() != null) {
            opportunity.adjustProbability(request.probability());
        }
        opportunity = opportunities.save(opportunity);
        recordHistory(opportunity, null);
        return OpportunityMapper.toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse update(Long id, OpportunityUpdateRequest request) {
        Opportunity opportunity = load(id);
        requireVersion(opportunity, request.version());
        opportunity.setTitle(request.title().trim());
        opportunity.setAmount(request.amount());
        opportunity.setExpectedCloseDate(request.expectedCloseDate());
        opportunity.setDescription(CompanyService.blankToNull(request.description()));
        opportunity.setPrimaryContact(contactOf(opportunity.getCompany(), request.primaryContactId()));
        if (request.probability() != null && !request.probability().equals(opportunity.getProbability())) {
            opportunity.adjustProbability(request.probability());
        }
        if (request.ownerId() != null && accessPolicy.user().hasGlobalVisibility()) {
            opportunity.setOwner(userService.resolveOwner(request.ownerId()));
        }
        opportunities.flush();
        return OpportunityMapper.toResponse(opportunity);
    }

    /** Arrastrar una tarjeta en el kanban. */
    @Transactional
    public OpportunityResponse changeStage(Long id, StageChangeRequest request) {
        Opportunity opportunity = load(id);
        requireVersion(opportunity, request.version());
        String from = opportunity.getStage().getCode();
        if (from.equals(request.stageCode())) {
            return OpportunityMapper.toResponse(opportunity);
        }
        opportunity.moveTo(stage(request.stageCode()), request.lostReason(), clock.instant());
        opportunities.flush();
        recordHistory(opportunity, from);
        return OpportunityMapper.toResponse(opportunity);
    }

    @Transactional
    public void delete(Long id) {
        opportunities.delete(load(id));
    }

    @Transactional(readOnly = true)
    public List<StageHistoryResponse> history(Long id) {
        load(id);
        List<OpportunityStageHistory> entries = history.findByOpportunityIdOrderByChangedAtAsc(id);
        Map<Long, String> names = new HashMap<>();
        users.findAllById(entries.stream().map(OpportunityStageHistory::getChangedBy).collect(Collectors.toSet()))
                .forEach(u -> names.put(u.getId(), u.getFullName()));
        return entries.stream().map(h -> OpportunityMapper.toResponse(h, names.get(h.getChangedBy()))).toList();
    }

    @Transactional(readOnly = true)
    public Opportunity load(Long id) {
        Opportunity opportunity = opportunities.findById(id)
                .orElseThrow(() -> new NotFoundException("Oportunidad no encontrada."));
        accessPolicy.checkOpportunity(opportunity);
        return opportunity;
    }

    private Contact contactOf(Company company, Long contactId) {
        if (contactId == null) {
            return null;
        }
        Contact contact = contactService.load(contactId, AuditAction.VIEW);
        if (!contact.getCompany().getId().equals(company.getId())) {
            throw new BusinessRuleException("El contacto debe pertenecer a la empresa de la oportunidad.");
        }
        return contact;
    }

    private PipelineStage stage(String code) {
        return stages.findById(code).orElseThrow(() -> new BusinessRuleException("La etapa no existe."));
    }

    /** Bloqueo optimista explícito: el cliente envía la versión que vio. */
    private static void requireVersion(Opportunity opportunity, Long expected) {
        if (!Objects.equals(opportunity.getVersion(), expected)) {
            throw new ConflictException("Otro usuario modificó esta oportunidad. Recargue el tablero.");
        }
    }

    private void recordHistory(Opportunity opportunity, String fromStage) {
        Instant now = clock.instant();
        history.save(new OpportunityStageHistory(opportunity.getId(), fromStage, opportunity.getStage().getCode(),
                opportunity.getAmount(), opportunity.getProbability(), accessPolicy.user().id(), now));
    }
}
