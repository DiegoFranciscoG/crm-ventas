package ec.dfgranda.crm.domain;

import ec.dfgranda.crm.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuoteCalculationTest {

    private static VatRate rate(String code, String percentage) {
        VatRate rate = mock(VatRate.class);
        when(rate.getCode()).thenReturn(code);
        when(rate.getPercentage()).thenReturn(new BigDecimal(percentage));
        return rate;
    }

    @Test
    void computesLineAmountsWithHalfUpRounding() {
        QuoteItem item = new QuoteItem(null, "Instalación", new BigDecimal("1"), new BigDecimal("99.99"),
                BigDecimal.ZERO, rate("4", "15.00"));

        assertThat(item.getLineSubtotal()).isEqualByComparingTo("99.99");
        assertThat(item.getLineTax()).isEqualByComparingTo("15.00"); // 14.9985 → 15.00
        assertThat(item.getLineTotal()).isEqualByComparingTo("114.99");
    }

    @Test
    void appliesDiscountBeforeTax() {
        QuoteItem item = new QuoteItem(null, "Licencias", new BigDecimal("3"), new BigDecimal("180.00"),
                new BigDecimal("10"), rate("4", "15.00"));

        assertThat(item.getLineDiscount()).isEqualByComparingTo("54.00");
        assertThat(item.netAmount()).isEqualByComparingTo("486.00");
        assertThat(item.getLineTax()).isEqualByComparingTo("72.90");
    }

    @Test
    void quoteTotalsAddUpItems() {
        Quote quote = new Quote("COT-2030-000001", mock(Opportunity.class), LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 1, 31), mock(User.class));
        quote.replaceItems(List.of(
                new QuoteItem(null, "A", new BigDecimal("3"), new BigDecimal("180.00"), new BigDecimal("10"), rate("4", "15.00")),
                new QuoteItem(null, "B", new BigDecimal("2"), new BigDecimal("45.00"), BigDecimal.ZERO, rate("0", "0.00"))));

        assertThat(quote.getSubtotal()).isEqualByComparingTo("630.00");
        assertThat(quote.getDiscountTotal()).isEqualByComparingTo("54.00");
        assertThat(quote.getTaxTotal()).isEqualByComparingTo("72.90");
        assertThat(quote.getTotal()).isEqualByComparingTo("648.90");
        assertThat(quote.getItems()).extracting(QuoteItem::getLineNumber).containsExactly(1, 2);
    }

    @Test
    void rejectsInvalidValues() {
        VatRate vat = rate("4", "15.00");
        assertThatThrownBy(() -> new QuoteItem(null, "X", BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, vat))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new QuoteItem(null, "X", BigDecimal.ONE, new BigDecimal("-1"), BigDecimal.ZERO, vat))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new QuoteItem(null, "X", BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("101"), vat))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Quote("N", mock(Opportunity.class), LocalDate.of(2030, 2, 1), LocalDate.of(2030, 1, 1), mock(User.class)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
