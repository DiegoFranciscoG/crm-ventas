package ec.dfgranda.crm.support;

import java.security.SecureRandom;
import java.util.Base64;

/** Secretos generados al vuelo para los tests: no hay valores fijos en el repositorio. */
public final class TestSecrets {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static final String JWT = randomBase64(48);
    public static final String CRON = randomBase64(48);
    public static final String DATA_KEY = randomBase64(32);

    private TestSecrets() {
    }

    public static String randomBase64(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getEncoder().encodeToString(buffer);
    }
}
