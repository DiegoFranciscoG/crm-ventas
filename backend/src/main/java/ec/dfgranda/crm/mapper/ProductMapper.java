package ec.dfgranda.crm.mapper;

import ec.dfgranda.crm.domain.Product;
import ec.dfgranda.crm.dto.ProductResponse;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getUnitPrice(),
                p.getVatRate().getCode(), p.getVatRate().getPercentage(), p.isActive());
    }
}
