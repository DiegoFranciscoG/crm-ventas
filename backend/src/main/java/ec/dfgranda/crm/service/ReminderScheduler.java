package ec.dfgranda.crm.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job interno de recordatorios mientras la instancia está despierta. Se desactiva en los tests. */
@Component
@ConditionalOnProperty(name = "app.reminders.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private final ReminderService reminderService;

    public ReminderScheduler(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT5M")
    public void run() {
        reminderService.dispatchDue();
    }
}
