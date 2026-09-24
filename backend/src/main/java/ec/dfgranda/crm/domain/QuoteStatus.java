package ec.dfgranda.crm.domain;

/** Estados de una cotización; solo DRAFT es editable. */
public enum QuoteStatus {
    DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED
}
