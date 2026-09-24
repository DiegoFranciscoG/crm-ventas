package ec.dfgranda.crm.validation;

/**
 * Validación de cédula y RUC de Ecuador.
 *
 * <ul>
 *   <li>Cédula: 10 dígitos; provincia 01-24 (INEC #6) o 30 (nacidos en el exterior, Registro Civil #5);
 *       tercer dígito 0-5; verificador módulo 10 con coeficientes 2,1,2,1,2,1,2,1,2 (supuesto S1).</li>
 *   <li>RUC de persona natural: cédula válida + {@code 001} (SRI #4).</li>
 *   <li>RUC de sociedad (tercer dígito 9): provincia 01-24, termina en {@code 001}; verificador módulo 11 con
 *       coeficientes 4,3,2,7,6,5,4,3,2 en la posición 10. Por el comunicado del SRI (#3), si el verificador no
 *       coincide el número <b>no se rechaza</b>: los RUC con secuencial de 7 dígitos no tienen algoritmo público.</li>
 *   <li>RUC de entidad pública (tercer dígito 6): provincia 01-24; verificador módulo 11 con coeficientes
 *       3,2,7,6,5,4,3,2 en la posición 9; establecimiento de 4 dígitos distinto de 0000 (supuesto S3).</li>
 * </ul>
 */
public final class EcuadorianIdValidator {

    private static final int[] CEDULA_COEFFICIENTS = {2, 1, 2, 1, 2, 1, 2, 1, 2};
    private static final int[] PRIVATE_COEFFICIENTS = {4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final int[] PUBLIC_COEFFICIENTS = {3, 2, 7, 6, 5, 4, 3, 2};
    private static final int ABROAD_PROVINCE_CODE = 30;
    private static final int MAX_PROVINCE_CODE = 24;

    private EcuadorianIdValidator() {
    }

    public static boolean isValidCedula(String cedula) {
        return validateCedula(cedula).valid();
    }

    public static IdentificationResult validateCedula(String cedula) {
        if (!isDigits(cedula, 10)) {
            return IdentificationResult.invalid("La cédula debe tener exactamente 10 dígitos.");
        }
        int province = twoDigits(cedula, 0);
        if (!isDomesticProvince(province) && province != ABROAD_PROVINCE_CODE) {
            return IdentificationResult.invalid("Código de provincia inválido: debe estar entre 01 y 24, o ser 30.");
        }
        if (digit(cedula, 2) > 5) {
            return IdentificationResult.invalid("El tercer dígito de una cédula debe estar entre 0 y 5.");
        }
        int sum = 0;
        for (int i = 0; i < CEDULA_COEFFICIENTS.length; i++) {
            int product = digit(cedula, i) * CEDULA_COEFFICIENTS[i];
            sum += product >= 10 ? product - 9 : product;
        }
        int expected = (10 - sum % 10) % 10;
        if (expected != digit(cedula, 9)) {
            return IdentificationResult.invalid("El dígito verificador de la cédula no es correcto.");
        }
        return IdentificationResult.verified(null, "Cédula válida.");
    }

    public static IdentificationResult validateRuc(String ruc) {
        if (!isDigits(ruc, 13)) {
            return IdentificationResult.invalid("El RUC debe tener exactamente 13 dígitos.");
        }
        int thirdDigit = digit(ruc, 2);
        if (thirdDigit <= 5) {
            return validateNaturalPersonRuc(ruc);
        }
        if (thirdDigit == 6) {
            return validatePublicEntityRuc(ruc);
        }
        if (thirdDigit == 9) {
            return validatePrivateCompanyRuc(ruc);
        }
        return IdentificationResult.invalid("El tercer dígito del RUC debe ser 0-5 (persona natural), 6 (pública) o 9 (sociedad).");
    }

    private static IdentificationResult validateNaturalPersonRuc(String ruc) {
        IdentificationResult cedula = validateCedula(ruc.substring(0, 10));
        if (!cedula.valid()) {
            return IdentificationResult.invalid("RUC de persona natural con cédula inválida: " + cedula.message());
        }
        if (!ruc.endsWith("001")) {
            return IdentificationResult.invalid("El RUC de persona natural es la cédula seguida de 001.");
        }
        return IdentificationResult.verified(TaxpayerType.NATURAL_PERSON, "RUC de persona natural válido.");
    }

    private static IdentificationResult validatePublicEntityRuc(String ruc) {
        if (!isDomesticProvince(twoDigits(ruc, 0))) {
            return IdentificationResult.invalid("Código de provincia inválido: debe estar entre 01 y 24.");
        }
        if (ruc.endsWith("0000")) {
            return IdentificationResult.invalid("El establecimiento de un RUC público no puede ser 0000.");
        }
        if (modulo11(ruc, PUBLIC_COEFFICIENTS) == digit(ruc, 8)) {
            return IdentificationResult.verified(TaxpayerType.PUBLIC_ENTITY, "RUC de entidad pública válido.");
        }
        return IdentificationResult.unverifiedChecksum(TaxpayerType.PUBLIC_ENTITY,
                "Estructura válida, pero el dígito verificador no coincide con el módulo 11. Confírmelo en la consulta pública del SRI.");
    }

    private static IdentificationResult validatePrivateCompanyRuc(String ruc) {
        if (!isDomesticProvince(twoDigits(ruc, 0))) {
            return IdentificationResult.invalid("Código de provincia inválido: debe estar entre 01 y 24.");
        }
        if (!ruc.endsWith("001")) {
            return IdentificationResult.invalid("El RUC de una sociedad termina siempre en 001.");
        }
        if (modulo11(ruc, PRIVATE_COEFFICIENTS) == digit(ruc, 9)) {
            return IdentificationResult.verified(TaxpayerType.PRIVATE_COMPANY, "RUC de sociedad válido.");
        }
        return IdentificationResult.unverifiedChecksum(TaxpayerType.PRIVATE_COMPANY,
                "Estructura válida. El SRI no aplica módulo 11 a los RUC con secuencial de 7 dígitos; confírmelo en la consulta pública del SRI.");
    }

    /** Dígito verificador módulo 11: 11 − (Σ dígito × coeficiente mod 11); residuo 0 → 0; resultado 10 → imposible (-1). */
    private static int modulo11(String number, int[] coefficients) {
        int sum = 0;
        for (int i = 0; i < coefficients.length; i++) {
            sum += digit(number, i) * coefficients[i];
        }
        int remainder = sum % 11;
        if (remainder == 0) {
            return 0;
        }
        int check = 11 - remainder;
        return check == 10 ? -1 : check;
    }

    private static boolean isDomesticProvince(int code) {
        return code >= 1 && code <= MAX_PROVINCE_CODE;
    }

    private static boolean isDigits(String value, int length) {
        if (value == null || value.length() != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static int twoDigits(String value, int from) {
        return digit(value, from) * 10 + digit(value, from + 1);
    }

    private static int digit(String value, int index) {
        return value.charAt(index) - '0';
    }
}
