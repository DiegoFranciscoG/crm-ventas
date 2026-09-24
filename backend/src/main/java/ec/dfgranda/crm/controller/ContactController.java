package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.dto.ConsentRequest;
import ec.dfgranda.crm.dto.ConsentResponse;
import ec.dfgranda.crm.dto.ConsentRevokeRequest;
import ec.dfgranda.crm.dto.ContactCreateRequest;
import ec.dfgranda.crm.dto.ContactDataExport;
import ec.dfgranda.crm.dto.ContactResponse;
import ec.dfgranda.crm.dto.ContactSummary;
import ec.dfgranda.crm.dto.ContactUpdateRequest;
import ec.dfgranda.crm.dto.DataSubjectRequestCreate;
import ec.dfgranda.crm.dto.ErasureResponse;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.service.ContactService;
import ec.dfgranda.crm.service.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
@Tag(name = "Contactos y consentimientos", description = "Toda operación sobre datos personales queda en el log de accesos.")
public class ContactController {

    private final ContactService contactService;
    private final PrivacyService privacyService;

    public ContactController(ContactService contactService, PrivacyService privacyService) {
        this.contactService = contactService;
        this.privacyService = privacyService;
    }

    @GetMapping
    public PageResponse<ContactSummary> list(@RequestParam(required = false) Long companyId,
                                             @RequestParam(required = false) @Size(max = 100) String search,
                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return contactService.list(companyId, search, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle del contacto con su historial de consentimientos (la cédula va enmascarada)")
    public ContactResponse get(@PathVariable Long id) {
        return contactService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un contacto; exige el consentimiento inicial de contacto comercial con evidencia")
    public ContactResponse create(@Valid @RequestBody ContactCreateRequest request) {
        return contactService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rectifica o actualiza los datos del contacto (LOPDP Art. 14)")
    public ContactResponse update(@PathVariable Long id, @Valid @RequestBody ContactUpdateRequest request) {
        return contactService.update(id, request);
    }

    @PostMapping("/{id}/consents")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentResponse grantConsent(@PathVariable Long id, @Valid @RequestBody ConsentRequest request) {
        return contactService.grantConsent(id, request);
    }

    @PostMapping("/{id}/consents/{consentId}/revoke")
    @Operation(summary = "Revoca un consentimiento sin exigir justificación (no se borra: queda como evidencia)")
    public ConsentResponse revokeConsent(@PathVariable Long id, @PathVariable Long consentId,
                                         @Valid @RequestBody ConsentRevokeRequest request) {
        return contactService.revokeConsent(id, consentId, request.reason());
    }

    @PostMapping("/{id}/export")
    @Operation(summary = "Derecho de acceso y portabilidad: exporta todos los datos en JSON (gerente/administrador)")
    public ResponseEntity<ContactDataExport> export(@PathVariable Long id, @Valid @RequestBody DataSubjectRequestCreate request) {
        ContactDataExport export = privacyService.export(id, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("contacto-" + id + "-datos.json").build().toString())
                .body(export);
    }

    @PostMapping("/{id}/erasure")
    @Operation(summary = "Derecho de eliminación: borra al contacto y anonimiza sus referencias (gerente/administrador)")
    public ErasureResponse erase(@PathVariable Long id, @Valid @RequestBody DataSubjectRequestCreate request) {
        return privacyService.erase(id, request);
    }
}
