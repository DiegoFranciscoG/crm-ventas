package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.domain.AuditResourceType;
import ec.dfgranda.crm.dto.AuditLogResponse;
import ec.dfgranda.crm.dto.DataSubjectRequestResponse;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.service.AccessAuditService;
import ec.dfgranda.crm.service.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/privacy")
@Tag(name = "Privacidad (LOPDP)", description = "Solo gerente y administrador.")
public class PrivacyController {

    private final PrivacyService privacyService;
    private final AccessAuditService auditService;
    private final Clock clock;

    public PrivacyController(PrivacyService privacyService, AccessAuditService auditService, Clock clock) {
        this.privacyService = privacyService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @GetMapping("/requests")
    @Operation(summary = "Registro de solicitudes de derechos del titular (Reglamento LOPDP Art. 15)")
    public PageResponse<DataSubjectRequestResponse> requests(@RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return privacyService.listRequests(page, size);
    }

    @GetMapping("/access-log")
    @Operation(summary = "Log de accesos a datos personales; por defecto, los últimos 30 días")
    public PageResponse<AuditLogResponse> accessLog(@RequestParam(required = false) AuditResourceType resourceType,
                                                    @RequestParam(required = false) Long resourceId,
                                                    @RequestParam(required = false) Long userId,
                                                    @RequestParam(defaultValue = "false") boolean onlyDenied,
                                                    @RequestParam(required = false) Instant from,
                                                    @RequestParam(required = false) Instant to,
                                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Instant end = to != null ? to : clock.instant().plusSeconds(1);
        Instant start = from != null ? from : end.minus(Duration.ofDays(30));
        return auditService.search(resourceType, resourceId, userId, onlyDenied, start, end, page, size);
    }
}
