package ec.dfgranda.crm.mapper;

import ec.dfgranda.crm.domain.Quote;
import ec.dfgranda.crm.domain.QuoteItem;
import ec.dfgranda.crm.dto.QuoteItemResponse;
import ec.dfgranda.crm.dto.QuoteResponse;
import ec.dfgranda.crm.dto.QuoteSummary;
import ec.dfgranda.crm.dto.TaxLine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class QuoteMapper {

    private QuoteMapper() {
    }

    public static QuoteSummary toSummary(Quote q) {
        return new QuoteSummary(q.getId(), q.getNumber(), q.getOpportunity().getId(), q.getOpportunity().getTitle(),
                q.getOpportunity().getCompany().getLegalName(), q.getStatus(), q.getIssueDate(), q.getValidUntil(),
                q.getTotal());
    }

    public static QuoteResponse toResponse(Quote q) {
        return new QuoteResponse(q.getId(), q.getNumber(), q.getOpportunity().getId(), q.getOpportunity().getTitle(),
                q.getOpportunity().getCompany().getId(), q.getOpportunity().getCompany().getLegalName(),
                q.getContact() == null ? null : q.getContact().getId(),
                q.getContact() == null ? null : q.getContact().fullName(),
                q.getStatus(), q.getIssueDate(), q.getValidUntil(), q.getCurrency(), q.getSubtotal(),
                q.getDiscountTotal(), q.getTaxTotal(), q.getTotal(), taxBreakdown(q), q.getNotes(),
                q.getItems().stream().map(QuoteMapper::toResponse).toList(), q.getCreatedBy().getFullName(),
                q.getCreatedAt());
    }

    /** Base imponible e IVA agrupados por tarifa, de mayor a menor porcentaje. */
    public static List<TaxLine> taxBreakdown(Quote q) {
        Map<BigDecimal, TaxLine> byRate = new TreeMap<>((a, b) -> b.compareTo(a));
        for (QuoteItem item : q.getItems()) {
            byRate.merge(item.getVatPercentage(),
                    new TaxLine(item.getVatRateCode(), item.getVatPercentage(), item.netAmount(), item.getLineTax()),
                    (a, b) -> new TaxLine(a.vatRateCode(), a.percentage(), a.taxableBase().add(b.taxableBase()),
                            a.tax().add(b.tax())));
        }
        return new ArrayList<>(byRate.values());
    }

    private static QuoteItemResponse toResponse(QuoteItem i) {
        return new QuoteItemResponse(i.getLineNumber(), i.getProduct() == null ? null : i.getProduct().getId(),
                i.getProduct() == null ? null : i.getProduct().getSku(), i.getDescription(), i.getQuantity(),
                i.getUnitPrice(), i.getDiscountPercent(), i.getVatRateCode(), i.getVatPercentage(),
                i.getLineSubtotal(), i.getLineDiscount(), i.getLineTax(), i.getLineTotal());
    }
}
