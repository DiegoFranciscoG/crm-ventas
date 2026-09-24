package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.dto.BoardColumn;
import ec.dfgranda.crm.dto.OpportunityRequest;
import ec.dfgranda.crm.dto.OpportunityResponse;
import ec.dfgranda.crm.dto.OpportunityUpdateRequest;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.dto.StageChangeRequest;
import ec.dfgranda.crm.dto.StageHistoryResponse;
import ec.dfgranda.crm.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import java.util.List;

@RestController
@RequestMapping("/api/opportunities")
@Tag(name = "Oportunidades y kanban")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @GetMapping
    public PageResponse<OpportunityResponse> list(@RequestParam(required = false) @Size(max = 30) String stage,
                                                  @RequestParam(required = false) Long companyId,
                                                  @RequestParam(required = false) Long ownerId,
                                                  @RequestParam(required = false) @Size(max = 100) String search,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return opportunityService.list(stage, companyId, ownerId, search, page, size);
    }

    @GetMapping("/board")
    @Operation(summary = "Tablero kanban: columnas por etapa con totales y monto ponderado")
    public List<BoardColumn> board(@RequestParam(required = false) Long ownerId) {
        return opportunityService.board(ownerId);
    }

    @GetMapping("/{id}")
    public OpportunityResponse get(@PathVariable Long id) {
        return opportunityService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OpportunityResponse create(@Valid @RequestBody OpportunityRequest request) {
        return opportunityService.create(request);
    }

    @PutMapping("/{id}")
    public OpportunityResponse update(@PathVariable Long id, @Valid @RequestBody OpportunityUpdateRequest request) {
        return opportunityService.update(id, request);
    }

    @PatchMapping("/{id}/stage")
    @Operation(summary = "Mueve la oportunidad de etapa (drag and drop); 409 si otro usuario la modificó antes")
    public OpportunityResponse changeStage(@PathVariable Long id, @Valid @RequestBody StageChangeRequest request) {
        return opportunityService.changeStage(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        opportunityService.delete(id);
    }

    @GetMapping("/{id}/history")
    public List<StageHistoryResponse> history(@PathVariable Long id) {
        return opportunityService.history(id);
    }
}
