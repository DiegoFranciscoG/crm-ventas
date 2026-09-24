package ec.dfgranda.crm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI crmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CRM Ventas API")
                        .version("1.0.0")
                        .description("CRM B2B con pipeline kanban, cotizaciones en PDF, forecast y cumplimiento de la LOPDP de Ecuador. "
                                + "Obtenga un token en POST /api/auth/login y úselo con el botón Authorize.")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
