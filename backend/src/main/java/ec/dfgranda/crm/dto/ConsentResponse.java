package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.ConsentChannel;
import ec.dfgranda.crm.domain.LegalBasis;

import java.time.Instant;

public record ConsentResponse(Long id, String purposeCode, String purposeName, LegalBasis legalBasis,
                              ConsentChannel channel, String evidence, String privacyNoticeVersion, Instant grantedAt,
                              Instant expiresAt, Instant revokedAt, String revocationReason, boolean active,
                              String recordedBy) {
}
