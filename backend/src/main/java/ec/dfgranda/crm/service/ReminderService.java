package ec.dfgranda.crm.service;

import ec.dfgranda.crm.config.MailProperties;
import ec.dfgranda.crm.domain.Activity;
import ec.dfgranda.crm.repository.ActivityRepository;
import ec.dfgranda.crm.service.mail.EmailMessage;
import ec.dfgranda.crm.service.mail.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Envía los recordatorios vencidos al dueño de cada actividad (usuario interno, no al contacto). Se ejecuta con un
 * job interno y con un cron externo (GitHub Actions), porque Render free suspende el servicio sin tráfico.
 */
@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private static final int BATCH_SIZE = 50;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm", Locale.forLanguageTag("es-EC"))
                    .withZone(OpportunityService.BUSINESS_ZONE);

    private final ActivityRepository activities;
    private final EmailService emailService;
    private final MailProperties mailProperties;
    private final Clock clock;

    public ReminderService(ActivityRepository activities, EmailService emailService, MailProperties mailProperties,
                           Clock clock) {
        this.activities = activities;
        this.emailService = emailService;
        this.mailProperties = mailProperties;
        this.clock = clock;
    }

    /** @return cantidad de recordatorios procesados (enviados u omitidos por dominio reservado). */
    @Transactional
    public int dispatchDue() {
        Instant now = clock.instant();
        List<Activity> due = activities.lockDueReminders(now, BATCH_SIZE);
        int processed = 0;
        for (Activity activity : due) {
            try {
                emailService.send(toMessage(activity));
                activity.markReminderSent(now);
                processed++;
            } catch (RuntimeException e) {
                // Queda pendiente y se reintenta en la siguiente ejecución.
                log.warn("No se pudo enviar el recordatorio de la actividad {}: {}", activity.getId(), e.getClass().getSimpleName());
            }
        }
        if (processed > 0) {
            log.info("Recordatorios procesados: {}", processed);
        }
        return processed;
    }

    private EmailMessage toMessage(Activity activity) {
        String when = activity.getDueAt() == null ? "sin fecha límite" : DATE_FORMAT.format(activity.getDueAt());
        String company = activity.getCompany() == null ? "" : "Empresa: " + activity.getCompany().getLegalName() + "\n";
        String body = "Hola " + activity.getOwner().getFullName() + ",\n\n"
                + "Tienes una actividad pendiente:\n\n"
                + activity.getSubject() + "\n"
                + company
                + "Fecha: " + when + "\n\n"
                + "Ábrela en el CRM: " + mailProperties.frontendUrl() + "/activities\n\n"
                + "Este es un mensaje automático de CRM Ventas.";
        return new EmailMessage(activity.getOwner().getEmail(), "Recordatorio: " + activity.getSubject(), body);
    }
}
