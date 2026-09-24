package ec.dfgranda.crm.validation;

/**
 * Tipo de contribuyente según el tercer dígito del RUC (docs/investigacion.md #3, #4).
 */
public enum TaxpayerType {
    /** Tercer dígito 0-5: cédula + 001. */
    NATURAL_PERSON,
    /** Tercer dígito 6: entidad del sector público. */
    PUBLIC_ENTITY,
    /** Tercer dígito 9: sociedad privada o extranjero sin cédula. */
    PRIVATE_COMPANY
}
