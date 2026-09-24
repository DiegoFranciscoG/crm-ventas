package ec.dfgranda.crm.dto;

import jakarta.validation.constraints.Size;

/** La revocatoria no requiere justificación (LOPDP Art. 8): el motivo es opcional. */
public record ConsentRevokeRequest(@Size(max = 300) String reason) {
}
