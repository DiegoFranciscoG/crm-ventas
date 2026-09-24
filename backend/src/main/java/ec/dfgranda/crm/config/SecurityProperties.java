package ec.dfgranda.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Configuración de seguridad. Los secretos no tienen valor por defecto: si faltan o son débiles, la
 * aplicación no arranca (RFC 7518 §3.2: HS256 exige una clave de al menos 256 bits).
 */
@ConfigurationProperties("app.security")
public record SecurityProperties(
        String jwtSecret,
        Duration jwtTtl,
        List<String> corsAllowedOrigins,
        int loginMaxAttempts,
        Duration loginWindow,
        String cronToken) {

    private static final int MIN_SECRET_BYTES = 32;

    public SecurityProperties {
        requireStrongSecret("JWT_SECRET", jwtSecret);
        requireStrongSecret("CRON_TOKEN", cronToken);
        if (corsAllowedOrigins == null || corsAllowedOrigins.isEmpty()
                || corsAllowedOrigins.stream().anyMatch(origin -> origin.isBlank() || origin.contains("*"))) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS debe listar orígenes explícitos (sin '*').");
        }
        if (jwtTtl == null || jwtTtl.isNegative() || jwtTtl.isZero() || jwtTtl.compareTo(Duration.ofHours(2)) > 0) {
            throw new IllegalStateException("JWT_TTL debe ser positivo y de máximo 2 horas.");
        }
        if (loginMaxAttempts < 1 || loginWindow == null || loginWindow.isNegative() || loginWindow.isZero()) {
            throw new IllegalStateException("Configuración de rate limiting de login inválida.");
        }
    }

    private static void requireStrongSecret(String name, String value) {
        if (value == null || value.startsWith("${") || value.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(name + " no está definido o tiene menos de " + MIN_SECRET_BYTES + " bytes.");
        }
    }
}
