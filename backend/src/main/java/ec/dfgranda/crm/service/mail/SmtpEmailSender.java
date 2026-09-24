package ec.dfgranda.crm.service.mail;

import ec.dfgranda.crm.config.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** SMTP para desarrollo local con Mailpit (docker compose). Render free bloquea SMTP, por eso no se usa allí. */
class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    SmtpEmailSender(JavaMailSender mailSender, MailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(properties.fromName() + " <" + properties.fromAddress() + ">");
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.textBody());
        mailSender.send(mail);
    }
}
