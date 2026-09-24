package ec.dfgranda.crm.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingTest {

    @Test
    void keepsOnlyTheLastFourDigits() {
        assertThat(Masking.maskIdentification("2059999991")).isEqualTo("******9991");
        assertThat(Masking.maskIdentification("2059999991001")).isEqualTo("*********1001");
        assertThat(Masking.maskIdentification("123")).isEqualTo("***");
        assertThat(Masking.maskIdentification("")).isNull();
        assertThat(Masking.maskIdentification(null)).isNull();
    }
}
