package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.dto.CompanyRequest;
import ec.dfgranda.crm.dto.CompanyResponse;
import ec.dfgranda.crm.dto.CompanySummary;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/companies")
@Tag(name = "Empresas", description = "El vendedor solo ve las empresas de las que es dueño.")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(summary = "Lista paginada; el RUC de personas naturales se enmascara")
    public PageResponse<CompanySummary> list(@RequestParam(required = false) @Size(max = 100) String search,
                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return companyService.list(search, page, size);
    }

    @GetMapping("/{id}")
    public CompanyResponse get(@PathVariable Long id) {
        return companyService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una empresa; el RUC se valida con las reglas del SRI")
    public CompanyResponse create(@Valid @RequestBody CompanyRequest request) {
        return companyService.create(request);
    }

    @PutMapping("/{id}")
    public CompanyResponse update(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una empresa sin contactos ni oportunidades (gerente/administrador)")
    public void delete(@PathVariable Long id) {
        companyService.delete(id);
    }
}
