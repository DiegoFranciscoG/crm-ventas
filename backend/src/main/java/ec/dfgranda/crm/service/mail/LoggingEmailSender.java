package ec.dfgranda.crm.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** No envía nada: registra que se habría enviado, sin destinatario ni asunto (pueden contener datos personales). */
class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("[MAIL_PROVIDER=log] Correo no enviado ({} caracteres de contenido).", message.textBody().length());
    }
}
