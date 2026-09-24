package ec.dfgranda.crm.exception;

/** Conflicto con el estado actual (duplicados, dependencias, edición concurrente): se responde 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
