package ec.dfgranda.crm.security;

import ec.dfgranda.crm.config.SecurityProperties;
import ec.dfgranda.crm.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private static final String SECRET = "x".repeat(40);

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2030-01-01T00:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginRateLimiter limiter = new LoginRateLimiter(new SecurityProperties(SECRET, Duration.ofMinutes(30),
            List.of("http://localhost:4200"), 3, Duration.ofMinutes(15), SECRET), clock);

    @Test
    void blocksAfterMaxFailuresAndUnblocksWhenTheWindowExpires() {
        for (int i = 0; i < 3; i++) {
            limiter.checkAllowed("a@x.ec", "1.1.1.1");
            limiter.recordFailure("a@x.ec", "1.1.1.1");
        }
        assertThatThrownBy(() -> limiter.checkAllowed("a@x.ec", "1.1.1.1")).isInstanceOf(TooManyRequestsException.class);

        clock.advance(Duration.ofMinutes(15));
        assertThatCode(() -> limiter.checkAllowed("a@x.ec", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void successResetsTheAccountCounter() {
        limiter.recordFailure("b@x.ec", "2.2.2.2");
        limiter.recordFailure("b@x.ec", "2.2.2.2");
        limiter.recordSuccess("b@x.ec");
        limiter.recordFailure("b@x.ec", "2.2.2.2");
        assertThatCode(() -> limiter.checkAllowed("b@x.ec", "2.2.2.2")).doesNotThrowAnyException();
    }

    @Test
    void limitsPasswordSprayingFromOneIp() {
        for (int i = 0; i < 12; i++) {
            limiter.recordFailure("user" + i + "@x.ec", "3.3.3.3");
        }
        assertThatThrownBy(() -> limiter.checkAllowed("otro@x.ec", "3.3.3.3")).isInstanceOf(TooManyRequestsException.class);
    }
}
