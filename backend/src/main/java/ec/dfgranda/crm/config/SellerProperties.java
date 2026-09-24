package ec.dfgranda.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos de la empresa que emite las cotizaciones (responsable del tratamiento, LOPDP Art. 12 num. 8).
 * Se imprimen en el PDF y en el aviso de privacidad.
 */
@ConfigurationProperties("app.seller")
public record SellerProperties(String legalName, String ruc, String address, String city, String phone,
                               String email, int quoteValidityDays) {

    public SellerProperties {
        if (quoteValidityDays < 1) {
            throw new IllegalStateException("app.seller.quote-validity-days debe ser mayor que cero.");
        }
    }
}
