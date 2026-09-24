package ec.dfgranda.crm.mapper;

/** Enmascara identificaciones personales: solo se muestran los últimos 4 dígitos (minimización, LOPDP Art. 10 e). */
public final class Masking {

    private static final int VISIBLE = 4;

    private Masking() {
    }

    public static String maskIdentification(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.length() <= VISIBLE) {
            return "*".repeat(value.length());
        }
        return "*".repeat(value.length() - VISIBLE) + value.substring(value.length() - VISIBLE);
    }
}
