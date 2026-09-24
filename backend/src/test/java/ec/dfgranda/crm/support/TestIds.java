package ec.dfgranda.crm.support;

import java.util.concurrent.atomic.AtomicInteger;

/** Genera RUC sintéticos de sociedades (provincia 24, no usada por los datos demo) únicos por ejecución. */
public final class TestIds {

    private static final int[] COEFFICIENTS = {4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final AtomicInteger SEQUENCE = new AtomicInteger(100_000 + (int) (System.nanoTime() % 400_000));

    private TestIds() {
    }

    public static String uniquePrivateRuc() {
        while (true) {
            String base = "249" + String.format("%06d", SEQUENCE.getAndIncrement());
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += (base.charAt(i) - '0') * COEFFICIENTS[i];
            }
            int remainder = sum % 11;
            int check = remainder == 0 ? 0 : 11 - remainder;
            if (check < 10) {
                return base + check + "001";
            }
        }
    }
}
