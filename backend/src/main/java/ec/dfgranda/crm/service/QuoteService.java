package ec.dfgranda.crm.service;

import ec.dfgranda.crm.config.SellerProperties;
import ec.dfgranda.crm.domain.AuditAction;
import ec.dfgranda.crm.domain.AuditResourceType;
import ec.dfgranda.crm.domain.Contact;
import ec.dfgranda.crm.domain.Opportunity;
import ec.dfgranda.crm.domain.ProcessingPurpose;
import ec.dfgranda.crm.domain.Product;
import ec.dfgranda.crm.domain.Quote;
import ec.dfgranda.crm.domain.QuoteItem;
import ec.dfgranda.crm.domain.QuoteStatus;
import ec.dfgranda.crm.domain.User;
import ec.dfgranda.crm.domain.VatRate;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.dto.QuoteItemRequest;
import ec.dfgranda.crm.dto.QuoteRequest;
import ec.dfgranda.crm.dto.QuoteResponse;
import ec.dfgranda.crm.dto.QuoteSummary;
import ec.dfgranda.crm.exception.BusinessRuleException;
import ec.dfgranda.crm.exception.NotFoundException;
import ec.dfgranda.crm.mapper.QuoteMapper;
import ec.dfgranda.crm.repository.ProductRepository;
import ec.dfgranda.crm.repository.QuoteRepository;
import ec.dfgranda.crm.repository.Specs;
import ec.dfgranda.crm.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Cotizaciones: totales calculados en el servidor, numeración secuencial y ciclo de vida controlado. */
@Service
public class QuoteService {

    /** Transiciones permitidas; ACCEPTED, REJECTED y EXPIRED son finales. */
    private static final Map<QuoteStatus, Set<QuoteStatus>> TRANSITIONS = Map.of(
            QuoteStatus.DRAFT, EnumSet.of(QuoteStatus.SENT),
            QuoteStatus.SENT, EnumSet.of(QuoteStatus.ACCEPTED, QuoteStatus.REJECTED, QuoteStatus.EXPIRED, QuoteStatus.DRAFT),
            QuoteStatus.ACCEPTED, EnumSet.noneOf(QuoteStatus.class),
            QuoteStatus.REJECTED, EnumSet.noneOf(QuoteStatus.class),
            QuoteStatus.EXPIRED, EnumSet.noneOf(QuoteStatus.class));

    private final QuoteRepository quotes;
    private final ProductRepository products;
    private final UserRepository users;
    private final OpportunityService opportunityService;
    private final ContactService contactService;
    private final ConsentService consentService;
    private final ProductService productService;
    private final QuotePdfRenderer pdfRenderer;
    private final AccessPolicy accessPolicy;
    private final AccessAuditService audit;
    private final SellerProperties seller;
    private final Clock clock;

    public QuoteService(QuoteRepository quotes, ProductRepository products, UserRepository users,
                        OpportunityService opportunityService, ContactService contactService,
                        ConsentService consentService, ProductService productService, QuotePdfRenderer pdfRenderer,
                        AccessPolicy accessPolicy, AccessAuditService audit, SellerProperties seller, Clock clock) {
        this.quotes = quotes;
        this.products = products;
        this.users = users;
        this.opportunityService = opportunityService;
        this.contactService = contactService;
        this.consentService = consentService;
        this.productService = productService;
        this.pdfRenderer = pdfRenderer;
        this.accessPolicy = accessPolicy;
        this.audit = audit;
        this.seller = seller;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<QuoteSummary> list(Long opportunityId, int page, int size) {
        if (opportunityId != null) {
            opportunityService.load(opportunityId);
        }
        return PageResponse.of(quotes.findAll(
                Specs.all(Specs.quoteOwnedBy(accessPolicy.ownerScope()), Specs.quoteOfOpportunity(opportunityId)),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issueDate", "id"))), QuoteMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public QuoteResponse get(Long id) {
        return QuoteMapper.toResponse(load(id, AuditAction.VIEW));
    }

    @Transactional
    public QuoteResponse create(QuoteRequest request) {
        Opportunity opportunity = opportunityService.load(request.opportunityId());
        if (opportunity.getStage().isClosed()) {
            throw new BusinessRuleException("No se puede cotizar una oportunidad cerrada.");
        }
        LocalDate issueDate = request.issueDate() != null ? request.issueDate() : today();
        LocalDate validUntil = request.validUntil() != null ? request.validUntil() : issueDate.plusDays(seller.quoteValidityDays());
        User author = users.getReferenceById(accessPolicy.user().id());
        Quote quote = new Quote(nextNumber(issueDate), opportunity, issueDate, validUntil, author);
        quote.setContact(recipient(opportunity, request.contactId()));
        quote.setNotes(CompanyService.blankToNull(request.notes()));
        quote.replaceItems(buildItems(request.items()));
        return QuoteMapper.toResponse(quotes.save(quote));
    }

    /** Solo los borradores son editables. */
    @Transactional
    public QuoteResponse update(Long id, QuoteRequest request) {
        Quote quote = load(id, AuditAction.UPDATE);
        if (!quote.isEditable()) {
            throw new BusinessRuleException("Solo se pueden editar cotizaciones en borrador.");
        }
        if (!quote.getOpportunity().getId().equals(request.opportunityId())) {
            throw new BusinessRuleException("No se puede mover una cotización a otra oportunidad.");
        }
        LocalDate issueDate = request.issueDate() != null ? request.issueDate() : quote.getIssueDate();
        LocalDate validUntil = request.validUntil() != null ? request.validUntil() : quote.getValidUntil();
        quote.changeValidity(issueDate, validUntil);
        quote.setContact(recipient(quote.getOpportunity(), request.contactId()));
        quote.setNotes(CompanyService.blankToNull(request.notes()));
        List<QuoteItem> items = buildItems(request.items());
        // Hibernate ejecuta los INSERT antes que los DELETE: se vacían y persisten primero los ítems anteriores
        // para no violar la unicidad (quote_id, line_number).
        quote.replaceItems(List.of());
        quotes.flush();
        quote.replaceItems(items);
        return QuoteMapper.toResponse(quote);
    }

    @Transactional
    public QuoteResponse changeStatus(Long id, QuoteStatus target) {
        Quote quote = load(id, AuditAction.UPDATE);
        if (!TRANSITIONS.get(quote.getStatus()).contains(target)) {
            throw new BusinessRuleException("Transición de estado no permitida: " + quote.getStatus() + " → " + target + ".");
        }
        if (target == QuoteStatus.SENT && quote.getContact() != null) {
            consentService.requireActive(quote.getContact(), ProcessingPurpose.QUOTE_DELIVERY, "enviar la cotización");
        }
        quote.changeStatus(target);
        return QuoteMapper.toResponse(quote);
    }

    /** PDF bajo demanda; si incluye datos del contacto, la descarga queda registrada. */
    @Transactional
    public RenderedPdf pdf(Long id) {
        Quote quote = load(id, AuditAction.PDF_DOWNLOAD);
        if (quote.getContact() != null) {
            audit.record(AuditAction.PDF_DOWNLOAD, AuditResourceType.QUOTE, quote.getId(), "contactId=" + quote.getContact().getId());
        }
        return new RenderedPdf(quote.getNumber() + ".pdf", pdfRenderer.render(quote));
    }

    private Quote load(Long id, AuditAction action) {
        Quote quote = quotes.findById(id).orElseThrow(() -> new NotFoundException("Cotización no encontrada."));
        accessPolicy.checkQuote(quote, action);
        return quote;
    }

    /** El destinatario debe ser de la empresa y haber autorizado el envío de cotizaciones. */
    private Contact recipient(Opportunity opportunity, Long contactId) {
        if (contactId == null) {
            return null;
        }
        Contact contact = contactService.load(contactId, AuditAction.VIEW);
        if (!contact.getCompany().getId().equals(opportunity.getCompany().getId())) {
            throw new BusinessRuleException("El contacto debe pertenecer a la empresa de la oportunidad.");
        }
        consentService.requireActive(contact, ProcessingPurpose.QUOTE_DELIVERY, "dirigir la cotización a este contacto");
        return contact;
    }

    private List<QuoteItem> buildItems(List<QuoteItemRequest> requests) {
        return requests.stream().map(this::buildItem).toList();
    }

    private QuoteItem buildItem(QuoteItemRequest request) {
        BigDecimal discount = request.discountPercent() == null ? BigDecimal.ZERO : request.discountPercent();
        if (request.productId() != null) {
            Product product = products.findById(request.productId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new BusinessRuleException("El producto no existe o está inactivo."));
            String description = request.description() == null || request.description().isBlank()
                    ? product.getName() : request.description().trim();
            BigDecimal price = request.unitPrice() != null ? request.unitPrice() : product.getUnitPrice();
            VatRate vat = productService.activeVatRate(
                    request.vatRateCode() != null ? request.vatRateCode() : product.getVatRate().getCode());
            return new QuoteItem(product, description, request.quantity(), price, discount, vat);
        }
        if (request.description() == null || request.description().isBlank() || request.unitPrice() == null
                || request.vatRateCode() == null) {
            throw new BusinessRuleException("Un ítem sin producto requiere descripción, precio y tarifa de IVA.");
        }
        return new QuoteItem(null, request.description().trim(), request.quantity(), request.unitPrice(), discount,
                productService.activeVatRate(request.vatRateCode()));
    }

    private String nextNumber(LocalDate issueDate) {
        return String.format("COT-%d-%06d", issueDate.getYear(), quotes.nextNumber());
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(OpportunityService.BUSINESS_ZONE));
    }

    public record RenderedPdf(String fileName, byte[] content) {
    }
}
