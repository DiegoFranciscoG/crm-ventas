package ec.dfgranda.crm.service.mail;

/** Correo de texto plano (sin HTML: evita inyección de contenido en el cliente de correo). */
public record EmailMessage(String to, String subject, String textBody) {
}
