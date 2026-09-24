package ec.dfgranda.crm.exception;

/**
 * Recurso inexistente o sin permiso para verlo. Se responde 404 en ambos casos para no revelar que un objeto
 * ajeno existe (OWASP API1:2023, BOLA).
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
