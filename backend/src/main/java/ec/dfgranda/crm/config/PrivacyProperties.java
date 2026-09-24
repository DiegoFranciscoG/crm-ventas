package ec.dfgranda.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Base64;

/**
 * Configuración de protección de datos. {@code dataEncryptionKey}: 32 bytes en Base64 (AES-256);
 * {@code requestDueDays}: plazo para atender derechos del titular (LOPDP Arts. 13-16: 15 días; supuesto S2).
 */
@ConfigurationProperties("app.privacy")
public record PrivacyProperties(String dataEncryptionKey, int requestDueDays, String privacyNoticeVersion) {

    public PrivacyProperties {
        if (dataEncryptionKey == null || dataEncryptionKey.startsWith("${")) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY no está definido.");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(dataEncryptionKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY debe estar en Base64.", e);
        }
        if (key.length != 32) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY debe decodificar a 32 bytes (AES-256).");
        }
        if (requestDueDays < 1 || requestDueDays > 15) {
            throw new IllegalStateException("app.privacy.request-due-days debe estar entre 1 y 15 (LOPDP).");
        }
        if (privacyNoticeVersion == null || privacyNoticeVersion.isBlank()) {
            throw new IllegalStateException("app.privacy.privacy-notice-version es obligatorio.");
        }
    }

    public byte[] encryptionKeyBytes() {
        return Base64.getDecoder().decode(dataEncryptionKey);
    }
}
