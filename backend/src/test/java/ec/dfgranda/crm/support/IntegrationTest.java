package ec.dfgranda.crm.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Base de los tests de integración: contexto completo + PostgreSQL en contenedor + secretos aleatorios. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    @DynamicPropertySource
    static void secrets(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt-secret", () -> TestSecrets.JWT);
        registry.add("app.security.cron-token", () -> TestSecrets.CRON);
        registry.add("app.security.cors-allowed-origins", () -> "http://localhost:4200");
        registry.add("app.privacy.data-encryption-key", () -> TestSecrets.DATA_KEY);
    }
}
