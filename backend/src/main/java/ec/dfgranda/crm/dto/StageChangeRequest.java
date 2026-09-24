package ec.dfgranda.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Movimiento en el kanban. {@code lostReason} es obligatorio al pasar a CLOSED_LOST. */
public record StageChangeRequest(
        @NotBlank @Size(max = 30) String stageCode,
        @Size(max = 300) String lostReason,
        @NotNull Long version) {
}
