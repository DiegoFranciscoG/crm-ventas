package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.dto.ForecastResponse;
import ec.dfgranda.crm.service.ForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/forecast")
@Tag(name = "Forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    @Operation(summary = "Pipeline, ponderado, commit, best case, ganado y tasa de cierre; el vendedor ve solo lo suyo")
    public ForecastResponse forecast(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(required = false) Long ownerId) {
        return forecastService.forecast(from, to, ownerId);
    }
}
