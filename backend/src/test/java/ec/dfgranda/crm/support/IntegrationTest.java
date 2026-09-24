package ec.dfgranda.crm.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Base de los tests de integración: contexto completo + PostgreSQL en contenedor + secretos aleatorios. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    public static final String ADMIN_EMAIL = "admin-test@example.com";
    public static final String MANAGER_EMAIL = "gerente@example.com";
    public static final String REP1_EMAIL = "vendedor1@example.com";
    public static final String REP2_EMAIL = "vendedor2@example.com";
    /** Contraseña de los usuarios demo (publicada en el README; no es un secreto). */
    public static final String DEMO_PASSWORD = "Demo-CRM-2026";

    @DynamicPropertySource
    static void secrets(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt-secret", () -> TestSecrets.JWT);
        registry.add("app.security.cron-token", () -> TestSecrets.CRON);
        registry.add("app.security.cors-allowed-origins", () -> "http://localhost:4200");
        registry.add("app.privacy.data-encryption-key", () -> TestSecrets.DATA_KEY);
        // Los datos demo también se migran en los tests: así se valida que los seeds cargan sin errores.
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/demo");
        registry.add("app.reminders.scheduler-enabled", () -> "false");
        // El administrador de los tests lo crea AdminBootstrap con una contraseña aleatoria.
        registry.add("ADMIN_EMAIL", () -> ADMIN_EMAIL);
        registry.add("ADMIN_PASSWORD", () -> TestSecrets.ADMIN_PASSWORD);
    }
}
