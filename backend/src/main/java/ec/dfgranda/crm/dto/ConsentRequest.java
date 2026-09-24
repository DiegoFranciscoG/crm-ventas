package ec.dfgranda.crm.dto;

import ec.dfgranda.crm.domain.ConsentChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** El consentimiento debe poder demostrarse: canal y evidencia son obligatorios (Reglamento LOPDP Art. 5). */
public record ConsentRequest(
        @NotBlank @Size(max = 30) String purposeCode,
        @NotNull ConsentChannel channel,
        @NotBlank @Size(min = 10, max = 500) String evidence) {
}
