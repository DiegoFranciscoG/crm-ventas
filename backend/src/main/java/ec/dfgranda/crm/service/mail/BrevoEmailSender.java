package ec.dfgranda.crm.service.mail;

import ec.dfgranda.crm.config.MailProperties;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * API transaccional de Brevo por HTTPS (investigación #15, #16): plan gratuito de 300 correos/día. La clave vive
 * solo en el backend (variable de entorno), nunca en el frontend.
 */
class BrevoEmailSender implements EmailSender {

    private final RestClient client;
    private final MailProperties properties;

    BrevoEmailSender(RestClient client, MailProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void send(EmailMessage message) {
        BrevoRequest body = new BrevoRequest(new Party(properties.fromName(), properties.fromAddress()),
                List.of(new Party(null, message.to())), message.subject(), message.textBody());
        client.post()
                .uri(properties.brevoApiUrl())
                .header("api-key", properties.brevoApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    record Party(String name, String email) {
    }

    record BrevoRequest(Party sender, List<Party> to, String subject, String textContent) {
    }
}
