package ec.dfgranda.crm.security;

import ec.dfgranda.crm.config.PrivacyProperties;
import ec.dfgranda.crm.support.TestSecrets;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldEncryptorTest {

    private static FieldEncryptor encryptor(String key) {
        return new FieldEncryptor(new PrivacyProperties(key, 15, "v1.0"));
    }

    private final FieldEncryptor encryptor = encryptor(TestSecrets.randomBase64(32));

    @Test
    void roundTripsAndUsesARandomIv() {
        String first = encryptor.encrypt("2059999991");
        String second = encryptor.encrypt("2059999991");

        assertThat(first).startsWith("v1:").isNotEqualTo(second);
        assertThat(encryptor.decrypt(first)).isEqualTo("2059999991");
        assertThat(encryptor.encrypt(null)).isNull();
        assertThat(encryptor.decrypt(null)).isNull();
    }

    @Test
    void detectsTamperingAndWrongKeys() {
        String encrypted = encryptor.encrypt("2059999991");
        byte[] payload = Base64.getDecoder().decode(encrypted.substring(3));
        payload[payload.length - 1] ^= 1;
        String tampered = "v1:" + Base64.getEncoder().encodeToString(payload);

        assertThatThrownBy(() -> encryptor.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> encryptor(TestSecrets.randomBase64(32)).decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> encryptor.decrypt("texto-plano")).isInstanceOf(IllegalStateException.class);
    }
}
