package ec.dfgranda.crm.domain;

import ec.dfgranda.crm.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Línea de cotización. Guarda la tarifa de IVA aplicada (código SRI y porcentaje) para que el documento no
 * cambie si luego se modifica la tarifa general (investigación #8).
 */
@Entity
@Table(name = "quote_items")
public class QuoteItem {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "vat_rate_code", nullable = false, length = 2)
    private String vatRateCode;

    @Column(name = "vat_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatPercentage;

    @Column(name = "line_subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineSubtotal;

    @Column(name = "line_discount", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineDiscount;

    @Column(name = "line_tax", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTax;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    protected QuoteItem() {
    }

    /**
     * bruto = cantidad × precio; descuento = bruto × %desc; neto = bruto − descuento; IVA = neto × %IVA;
     * total = neto + IVA. Redondeo HALF_UP a 2 decimales en cada paso, como en los comprobantes del SRI.
     */
    public QuoteItem(Product product, String description, BigDecimal quantity, BigDecimal unitPrice,
                     BigDecimal discountPercent, VatRate vatRate) {
        if (quantity.signum() <= 0) {
            throw new BusinessRuleException("La cantidad debe ser mayor que cero.");
        }
        if (unitPrice.signum() < 0) {
            throw new BusinessRuleException("El precio unitario no puede ser negativo.");
        }
        if (discountPercent.signum() < 0 || discountPercent.compareTo(HUNDRED) > 0) {
            throw new BusinessRuleException("El descuento debe estar entre 0 y 100 %.");
        }
        this.product = product;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountPercent = discountPercent;
        this.vatRateCode = vatRate.getCode();
        this.vatPercentage = vatRate.getPercentage();
        this.lineSubtotal = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        this.lineDiscount = percentOf(lineSubtotal, discountPercent);
        BigDecimal net = lineSubtotal.subtract(lineDiscount);
        this.lineTax = percentOf(net, vatPercentage);
        this.lineTotal = net.add(lineTax);
    }

    private static BigDecimal percentOf(BigDecimal base, BigDecimal percent) {
        return base.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    void attachTo(Quote quote, int lineNumber) {
        this.quote = quote;
        this.lineNumber = lineNumber;
    }

    public BigDecimal netAmount() {
        return lineSubtotal.subtract(lineDiscount);
    }

    public Long getId() {
        return id;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Product getProduct() {
        return product;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public String getVatRateCode() {
        return vatRateCode;
    }

    public BigDecimal getVatPercentage() {
        return vatPercentage;
    }

    public BigDecimal getLineSubtotal() {
        return lineSubtotal;
    }

    public BigDecimal getLineDiscount() {
        return lineDiscount;
    }

    public BigDecimal getLineTax() {
        return lineTax;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
