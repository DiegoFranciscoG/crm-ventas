package ec.dfgranda.crm.service;

import ec.dfgranda.crm.domain.Product;
import ec.dfgranda.crm.domain.VatRate;
import ec.dfgranda.crm.dto.ProductRequest;
import ec.dfgranda.crm.dto.ProductResponse;
import ec.dfgranda.crm.exception.BusinessRuleException;
import ec.dfgranda.crm.exception.ConflictException;
import ec.dfgranda.crm.exception.NotFoundException;
import ec.dfgranda.crm.mapper.ProductMapper;
import ec.dfgranda.crm.repository.ProductRepository;
import ec.dfgranda.crm.repository.VatRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ProductService {

    private final ProductRepository products;
    private final VatRateRepository vatRates;

    public ProductService(ProductRepository products, VatRateRepository vatRates) {
        this.products = products;
        this.vatRates = vatRates;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list(boolean includeInactive) {
        List<Product> result = includeInactive ? products.findAllByOrderByNameAsc() : products.findByActiveTrueOrderByNameAsc();
        return result.stream().map(ProductMapper::toResponse).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (products.existsBySkuIgnoreCase(sku)) {
            throw new ConflictException("Ya existe un producto con ese SKU.");
        }
        Product product = new Product(sku, request.name().trim(), request.unitPrice(), activeVatRate(request.vatRateCode()));
        product.setDescription(CompanyService.blankToNull(request.description()));
        product.setActive(request.active());
        return ProductMapper.toResponse(products.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = products.findById(id).orElseThrow(() -> new NotFoundException("Producto no encontrado."));
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (products.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new ConflictException("Ya existe un producto con ese SKU.");
        }
        product.setSku(sku);
        product.setName(request.name().trim());
        product.setDescription(CompanyService.blankToNull(request.description()));
        product.setUnitPrice(request.unitPrice());
        product.setVatRate(activeVatRate(request.vatRateCode()));
        product.setActive(request.active());
        return ProductMapper.toResponse(product);
    }

    /** Solo se aceptan tarifas vigentes (Tabla 17 del SRI filtrada por el catálogo). */
    VatRate activeVatRate(String code) {
        return vatRates.findById(code).filter(VatRate::isActive)
                .orElseThrow(() -> new BusinessRuleException("La tarifa de IVA no existe o no está vigente."));
    }
}
