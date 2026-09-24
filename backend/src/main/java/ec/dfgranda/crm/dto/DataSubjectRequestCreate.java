package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.RequestChannel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de la solicitud del titular que se registra junto con la exportación o la eliminación. */
public record DataSubjectRequestCreate(@NotNull RequestChannel channel, @Size(max = 300) String notes) {
}
