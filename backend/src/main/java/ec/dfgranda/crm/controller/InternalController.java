package ec.dfgranda.crm.controller;

import ec.dfgranda.crm.config.SecurityProperties;
import ec.dfgranda.crm.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Endpoint para el cron externo (GitHub Actions): despierta la instancia de Render y despacha recordatorios.
 * Se protege con un token largo comparado en tiempo constante (no depende de un usuario).
 */
@RestController
@RequestMapping("/api/internal")
@Tag(name = "Interno")
public class InternalController {

    private final ReminderService reminderService;
    private final byte[] expectedToken;

    public InternalController(ReminderService reminderService, SecurityProperties properties) {
        this.reminderService = reminderService;
        this.expectedToken = properties.cronToken().getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/reminders/dispatch")
    @Operation(summary = "Despacha recordatorios vencidos (requiere la cabecera X-Cron-Token)")
    public ResponseEntity<Map<String, Integer>> dispatch(@RequestHeader("X-Cron-Token") String token) {
        if (!MessageDigest.isEqual(expectedToken, token.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("processed", reminderService.dispatchDue()));
    }
}
