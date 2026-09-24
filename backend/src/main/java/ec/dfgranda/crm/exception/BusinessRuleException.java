package ec.dfgranda.crm.exception;

/** Violación de una regla de negocio (se responde 422 con un mensaje legible para el usuario). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
