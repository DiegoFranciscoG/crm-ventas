package ec.dfgranda.crm.mapper;

import ec.dfgranda.crm.domain.Industry;
import ec.dfgranda.crm.domain.PipelineStage;
import ec.dfgranda.crm.domain.ProcessingPurpose;
import ec.dfgranda.crm.domain.Province;
import ec.dfgranda.crm.domain.VatRate;
import ec.dfgranda.crm.dto.CodeName;
import ec.dfgranda.crm.dto.PurposeResponse;
import ec.dfgranda.crm.dto.StageResponse;
import ec.dfgranda.crm.dto.VatRateResponse;

public final class CatalogMapper {

    private CatalogMapper() {
    }

    public static CodeName toCodeName(Province province) {
        return new CodeName(province.getCode(), province.getName());
    }

    public static CodeName toCodeName(Industry industry) {
        return new CodeName(industry.getCode(), industry.getName());
    }

    public static VatRateResponse toResponse(VatRate rate) {
        return new VatRateResponse(rate.getCode(), rate.getDescription(), rate.getPercentage());
    }

    public static StageResponse toResponse(PipelineStage stage) {
        return new StageResponse(stage.getCode(), stage.getName(), stage.getPosition(), stage.getDefaultProbability(),
                stage.getForecastCategory(), stage.isClosed(), stage.isWon());
    }

    public static PurposeResponse toResponse(ProcessingPurpose purpose) {
        return new PurposeResponse(purpose.getCode(), purpose.getName(), purpose.getDescription(),
                purpose.getLegalBasis(), purpose.getRetentionMonths());
    }
}
