package ec.dfgranda.crm.security;

import ec.dfgranda.crm.config.SecurityProperties;
import ec.dfgranda.crm.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos de login por cuenta y por IP en una ventana fija (OWASP A07:2025, API4:2023).
 * Contar por cuenta frena la fuerza bruta distribuida; contar por IP frena el password spraying.
 * Es en memoria: suficiente para una sola instancia (Render free). Con varias instancias iría a Redis.
 */
@Component
public class LoginRateLimiter {

    private static final int IP_MULTIPLIER = 4;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Window> failures = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;

    public LoginRateLimiter(SecurityProperties properties, Clock clock) {
        this.maxAttempts = properties.loginMaxAttempts();
        this.window = properties.loginWindow();
        this.clock = clock;
    }

    public void checkAllowed(String email, String ip) {
        Instant now = clock.instant();
        check("email:" + email, maxAttempts, now);
        check("ip:" + ip, maxAttempts * IP_MULTIPLIER, now);
    }

    public void recordFailure(String email, String ip) {
        Instant now = clock.instant();
        if (failures.size() > CLEANUP_THRESHOLD) {
            failures.values().removeIf(w -> w.isExpired(now, window));
        }
        increment("email:" + email, now);
        increment("ip:" + ip, now);
    }

    public void recordSuccess(String email) {
        failures.remove("email:" + email);
    }

    private void check(String key, int limit, Instant now) {
        Window current = failures.get(key);
        if (current != null && !current.isExpired(now, window) && current.count() >= limit) {
            Duration retryAfter = Duration.between(now, current.start().plus(window));
            throw new TooManyRequestsException(
                    "Demasiados intentos fallidos. Intente de nuevo en " + Math.max(1, retryAfter.toMinutes()) + " minuto(s).",
                    retryAfter);
        }
    }

    private void increment(String key, Instant now) {
        failures.compute(key, (k, current) ->
                current == null || current.isExpired(now, window) ? new Window(now, 1) : new Window(current.start(), current.count() + 1));
    }

    private record Window(Instant start, int count) {
        boolean isExpired(Instant now, Duration window) {
            return !now.isBefore(start.plus(window));
        }
    }
}
