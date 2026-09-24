package ec.dfgranda.crm.validation;

/**
 * Resultado de validar una cédula o un RUC.
 *
 * @param valid            la estructura cumple las reglas oficiales
 * @param taxpayerType     tipo de contribuyente (solo RUC; {@code null} para cédula o si es inválido)
 * @param checksumVerified el dígito verificador pudo comprobarse; en sociedades con secuencial de 7 dígitos
 *                         el SRI ya no aplica módulo 11 (investigación #3), por eso puede ser {@code false}
 *                         aunque el número sea válido
 * @param message          explicación legible para la interfaz
 */
public record IdentificationResult(boolean valid, TaxpayerType taxpayerType, boolean checksumVerified, String message) {

    static IdentificationResult invalid(String message) {
        return new IdentificationResult(false, null, false, message);
    }

    static IdentificationResult verified(TaxpayerType type, String message) {
        return new IdentificationResult(true, type, true, message);
    }

    static IdentificationResult unverifiedChecksum(TaxpayerType type, String message) {
        return new IdentificationResult(true, type, false, message);
    }
}
