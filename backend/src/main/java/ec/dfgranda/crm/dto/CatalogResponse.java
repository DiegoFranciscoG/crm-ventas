package ec.dfgranda.crm.dto;

import java.util.List;

public record CatalogResponse(List<CodeName> provinces, List<CodeName> industries, List<VatRateResponse> vatRates,
                              List<StageResponse> stages, List<PurposeResponse> purposes, List<CodeName> roles,
                              String privacyNoticeVersion) {
}
