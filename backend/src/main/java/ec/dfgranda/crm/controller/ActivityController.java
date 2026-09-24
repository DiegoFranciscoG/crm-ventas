package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.dto.ActivityRequest;
import ec.dfgranda.crm.dto.ActivityResponse;
import ec.dfgranda.crm.dto.PageResponse;
import ec.dfgranda.crm.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.time.Instant;

@RestController
@RequestMapping("/api/activities")
@Tag(name = "Actividades y recordatorios")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public PageResponse<ActivityResponse> list(@RequestParam(required = false) Long opportunityId,
                                               @RequestParam(required = false) Long companyId,
                                               @RequestParam(required = false) Long contactId,
                                               @RequestParam(required = false) Boolean completed,
                                               @RequestParam(required = false) Instant from,
                                               @RequestParam(required = false) Instant to,
                                               @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return activityService.list(opportunityId, companyId, contactId, completed, from, to, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una actividad; llamadas, reuniones y emails a un contacto requieren su consentimiento vigente")
    public ActivityResponse create(@Valid @RequestBody ActivityRequest request) {
        return activityService.create(request);
    }

    @PutMapping("/{id}")
    public ActivityResponse update(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        return activityService.update(id, request);
    }

    @PostMapping("/{id}/complete")
    public ActivityResponse complete(@PathVariable Long id) {
        return activityService.complete(id, true);
    }

    @PostMapping("/{id}/reopen")
    public ActivityResponse reopen(@PathVariable Long id) {
        return activityService.complete(id, false);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        activityService.delete(id);
    }
}
