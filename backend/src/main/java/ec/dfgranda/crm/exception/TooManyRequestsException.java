package ec.dfgranda.crm.exception;

import java.time.Duration;

/** Límite de intentos superado (rate limiting del login): se responde 429 con Retry-After. */
public class TooManyRequestsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyRequestsException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
