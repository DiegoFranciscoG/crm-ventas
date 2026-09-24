package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.ForecastCategory;

public record StageResponse(String code, String name, int position, int defaultProbability,
                            ForecastCategory forecastCategory, boolean closed, boolean won) {
}
