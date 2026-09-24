package ec.dfgranda.crm.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "quotes")
public class Quote extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuoteStatus status = QuoteStatus.DRAFT;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 2000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<QuoteItem> items = new ArrayList<>();

    protected Quote() {
    }

    public Quote(String number, Opportunity opportunity, LocalDate issueDate, LocalDate validUntil, User createdBy) {
        if (validUntil.isBefore(issueDate)) {
            throw new IllegalArgumentException("La validez de la cotización no puede ser anterior a su emisión.");
        }
        this.number = number;
        this.opportunity = opportunity;
        this.issueDate = issueDate;
        this.validUntil = validUntil;
        this.createdBy = createdBy;
    }

    public boolean isEditable() {
        return status == QuoteStatus.DRAFT;
    }

    /** Reemplaza los ítems y recalcula los totales en el servidor (nunca se aceptan totales del cliente). */
    public void replaceItems(List<QuoteItem> newItems) {
        items.clear();
        int line = 1;
        for (QuoteItem item : newItems) {
            item.attachTo(this, line++);
            items.add(item);
        }
        recalculateTotals();
    }

    private void recalculateTotals() {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal discounts = BigDecimal.ZERO;
        BigDecimal taxes = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        for (QuoteItem item : items) {
            gross = gross.add(item.getLineSubtotal());
            discounts = discounts.add(item.getLineDiscount());
            taxes = taxes.add(item.getLineTax());
            net = net.add(item.getLineSubtotal().subtract(item.getLineDiscount()));
        }
        this.subtotal = gross;
        this.discountTotal = discounts;
        this.taxTotal = taxes;
        this.total = net.add(taxes);
    }

    public void changeValidity(LocalDate issueDate, LocalDate validUntil) {
        if (validUntil.isBefore(issueDate)) {
            throw new IllegalArgumentException("La validez de la cotización no puede ser anterior a su emisión.");
        }
        this.issueDate = issueDate;
        this.validUntil = validUntil;
    }

    public void changeStatus(QuoteStatus newStatus) {
        this.status = newStatus;
    }

    /** Desvincula al titular eliminado; el documento comercial se conserva sin sus datos (LOPDP Art. 15). */
    public void detachErasedContact() {
        this.contact = null;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public BigDecimal getTaxTotal() {
        return taxTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public List<QuoteItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
