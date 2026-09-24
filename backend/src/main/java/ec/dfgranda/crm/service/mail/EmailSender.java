package ec.dfgranda.crm.service.mail;

/** Puerto de salida para enviar correos; la implementación depende de {@code MAIL_PROVIDER}. */
public interface EmailSender {

    void send(EmailMessage message);
}
