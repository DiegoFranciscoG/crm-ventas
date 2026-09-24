package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.dto.QuoteRequest;
import ec.dfgranda.crm.dto.QuoteResponse;
import ec.dfgranda.crm.dto.QuoteStatusRequest;
import ec.dfgranda.crm.dto.QuoteSummary;
import ec.dfgranda.crm.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotes")
@Tag(name = "Cotizaciones")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    public PageResponse<QuoteSummary> list(@RequestParam(required = false) Long opportunityId,
                                           @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return quoteService.list(opportunityId, page, size);
    }

    @GetMapping("/{id}")
    public QuoteResponse get(@PathVariable Long id) {
        return quoteService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una cotización; los totales e IVA los calcula el servidor")
    public QuoteResponse create(@Valid @RequestBody QuoteRequest request) {
        return quoteService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita una cotización en borrador")
    public QuoteResponse update(@PathVariable Long id, @Valid @RequestBody QuoteRequest request) {
        return quoteService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public QuoteResponse changeStatus(@PathVariable Long id, @Valid @RequestBody QuoteStatusRequest request) {
        return quoteService.changeStatus(id, request.status());
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Descarga la cotización en PDF")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        QuoteService.RenderedPdf pdf = quoteService.pdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.fileName()).build().toString())
                .body(pdf.content());
    }
}
