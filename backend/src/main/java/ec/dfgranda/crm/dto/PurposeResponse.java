package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.LegalBasis;

public record PurposeResponse(String code, String name, String description, LegalBasis legalBasis, int retentionMonths) {
}
