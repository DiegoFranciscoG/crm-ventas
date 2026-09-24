package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.dto.CatalogResponse;
import ec.dfgranda.crm.dto.IdentificationRequest;
import ec.dfgranda.crm.service.CatalogService;
import ec.dfgranda.crm.validation.EcuadorianIdValidator;
import ec.dfgranda.crm.validation.IdentificationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Catálogos y validación")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/catalogs")
    @Operation(summary = "Provincias (INEC), CIIU, tarifas de IVA (SRI), etapas del pipeline y finalidades LOPDP")
    public CatalogResponse catalogs() {
        return catalogService.all();
    }

    @PostMapping("/validation/identification")
    @Operation(summary = "Valida una cédula o un RUC con las reglas del SRI (sin consultar servicios externos)")
    public IdentificationResult validate(@Valid @RequestBody IdentificationRequest request) {
        return request.type() == IdentificationRequest.Type.RUC
                ? EcuadorianIdValidator.validateRuc(request.number())
                : EcuadorianIdValidator.validateCedula(request.number());
    }
}
