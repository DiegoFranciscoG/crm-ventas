package ec.dfgranda.crm.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La app no debe arrancar con secretos ausentes, débiles o con CORS abierto. */
class ConfigurationPropertiesTest {

    private static final String STRONG = "s".repeat(32);
    private static final List<String> ORIGINS = List.of("https://crm.example.com");

    @Test
    void acceptsStrongConfiguration() {
        assertThatCode(() -> new SecurityProperties(STRONG, Duration.ofMinutes(30), ORIGINS, 5, Duration.ofMinutes(15), STRONG))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingOrShortSecrets() {
        assertThatThrownBy(() -> new SecurityProperties(null, Duration.ofMinutes(30), ORIGINS, 5, Duration.ofMinutes(15), STRONG))
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new SecurityProperties("${JWT_SECRET}", Duration.ofMinutes(30), ORIGINS, 5, Duration.ofMinutes(15), STRONG))
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> new SecurityProperties(STRONG, Duration.ofMinutes(30), ORIGINS, 5, Duration.ofMinutes(15), "corto"))
                .hasMessageContaining("CRON_TOKEN");
    }

    @Test
    void rejectsWildcardCorsAndLongTokens() {
        assertThatThrownBy(() -> new SecurityProperties(STRONG, Duration.ofMinutes(30), List.of("*"), 5, Duration.ofMinutes(15), STRONG))
                .hasMessageContaining("CORS");
        assertThatThrownBy(() -> new SecurityProperties(STRONG, Duration.ofHours(8), ORIGINS, 5, Duration.ofMinutes(15), STRONG))
                .hasMessageContaining("JWT_TTL");
    }

    @Test
    void privacyKeyMustBe256Bits() {
        assertThatThrownBy(() -> new PrivacyProperties(null, 15, "v1.0")).hasMessageContaining("DATA_ENCRYPTION_KEY");
        assertThatThrownBy(() -> new PrivacyProperties("no-es-base64!", 15, "v1.0")).hasMessageContaining("Base64");
        assertThatThrownBy(() -> new PrivacyProperties("AAAA", 15, "v1.0")).hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> new PrivacyProperties("A".repeat(43) + "=", 30, "v1.0")).hasMessageContaining("15");
    }

    @Test
    void brevoRequiresAnApiKey() {
        assertThatThrownBy(() -> new MailProperties(MailProperties.Provider.BREVO, "a@b.ec", "CRM", "", "u", "f"))
                .hasMessageContaining("BREVO_API_KEY");
        assertThatCode(() -> new MailProperties(null, "a@b.ec", "CRM", null, "u", "f")).doesNotThrowAnyException();
    }
}
