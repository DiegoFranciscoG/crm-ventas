package ec.dfgranda.crm.domain;

import ec.dfgranda.crm.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpportunityStageTest {

    private static final Instant NOW = Instant.parse("2030-01-15T12:00:00Z");

    private static PipelineStage stage(int probability, boolean closed, boolean won) {
        PipelineStage stage = mock(PipelineStage.class);
        when(stage.getDefaultProbability()).thenReturn(probability);
        when(stage.isClosed()).thenReturn(closed);
        when(stage.isWon()).thenReturn(won);
        when(stage.isLost()).thenReturn(closed && !won);
        return stage;
    }

    private static Opportunity opportunity() {
        return new Opportunity("Proyecto", mock(Company.class), mock(User.class), stage(10, false, false),
                new BigDecimal("12345.67"), LocalDate.of(2030, 3, 1));
    }

    @Test
    void movingToAStageAppliesItsDefaultProbability() {
        Opportunity opportunity = opportunity();
        opportunity.moveTo(stage(75, false, false), null, NOW);

        assertThat(opportunity.getProbability()).isEqualTo(75);
        assertThat(opportunity.weightedAmount()).isEqualByComparingTo("9259.25");
        assertThat(opportunity.getClosedAt()).isNull();
    }

    @Test
    void closingRecordsTheDateAndLostRequiresAReason() {
        Opportunity opportunity = opportunity();
        assertThatThrownBy(() -> opportunity.moveTo(stage(0, true, false), "  ", NOW))
                .isInstanceOf(BusinessRuleException.class);

        opportunity.moveTo(stage(0, true, false), " Precio ", NOW);
        assertThat(opportunity.getClosedAt()).isEqualTo(NOW);
        assertThat(opportunity.getLostReason()).isEqualTo("Precio");

        opportunity.moveTo(stage(20, false, false), null, NOW);
        assertThat(opportunity.getClosedAt()).isNull();
        assertThat(opportunity.getLostReason()).isNull();
    }

    @Test
    void probabilityCanOnlyBeAdjustedWhileOpen() {
        Opportunity opportunity = opportunity();
        opportunity.adjustProbability(35);
        assertThat(opportunity.getProbability()).isEqualTo(35);
        assertThatThrownBy(() -> opportunity.adjustProbability(120)).isInstanceOf(BusinessRuleException.class);

        opportunity.moveTo(stage(100, true, true), null, NOW);
        assertThatThrownBy(() -> opportunity.adjustProbability(50)).isInstanceOf(BusinessRuleException.class);
    }
}
