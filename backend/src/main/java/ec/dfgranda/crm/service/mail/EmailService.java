package ec.dfgranda.crm.service.mail;

import ec.dfgranda.crm.config.MailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Punto único de envío. Con un proveedor real (Brevo) nunca envía a dominios reservados por el RFC 2606
 * (example.com, .test, .invalid...): los datos demo usan esos dominios y no deben generar rebotes.
 */
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final List<String> RESERVED_DOMAINS = List.of("example.com", "example.net", "example.org");
    private static final List<String> RESERVED_TLDS = List.of(".test", ".example", ".invalid", ".localhost");

    private final EmailSender sender;
    private final MailProperties.Provider provider;

    public EmailService(EmailSender sender, MailProperties.Provider provider) {
        this.sender = sender;
        this.provider = provider;
    }

    /** @return {@code true} si se entregó al proveedor; {@code false} si se omitió por dominio reservado. */
    public boolean send(EmailMessage message) {
        if (provider == MailProperties.Provider.BREVO && isReserved(message.to())) {
            log.info("Correo omitido: destinatario en dominio reservado (RFC 2606).");
            return false;
        }
        sender.send(message);
        return true;
    }

    static boolean isReserved(String address) {
        int at = address.lastIndexOf('@');
        String domain = (at < 0 ? address : address.substring(at + 1)).toLowerCase(Locale.ROOT);
        boolean reservedDomain = RESERVED_DOMAINS.stream().anyMatch(d -> domain.equals(d) || domain.endsWith("." + d));
        return reservedDomain || RESERVED_TLDS.stream().anyMatch(domain::endsWith);
    }
}
