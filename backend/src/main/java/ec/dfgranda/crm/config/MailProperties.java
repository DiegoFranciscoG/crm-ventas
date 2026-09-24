package ec.dfgranda.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Envío de correo. {@code smtp} para desarrollo (Mailpit), {@code brevo} en producción: Render free bloquea
 * los puertos SMTP y se usa la API HTTPS (investigación #11, #12, #16). {@code log} no envía nada.
 */
@ConfigurationProperties("app.mail")
public record MailProperties(Provider provider, String fromAddress, String fromName, String brevoApiKey,
                             String brevoApiUrl, String frontendUrl) {

    public enum Provider { LOG, SMTP, BREVO }

    public MailProperties {
        if (provider == null) {
            provider = Provider.LOG;
        }
        if (provider == Provider.BREVO && (brevoApiKey == null || brevoApiKey.isBlank())) {
            throw new IllegalStateException("MAIL_PROVIDER=brevo requiere BREVO_API_KEY.");
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("MAIL_FROM es obligatorio.");
        }
    }
}
