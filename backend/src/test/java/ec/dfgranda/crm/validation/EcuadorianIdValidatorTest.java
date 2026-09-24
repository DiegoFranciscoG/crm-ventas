package ec.dfgranda.crm.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos calculados a mano. Las cédulas son sintéticas y usan rangos no asignados (provincias 20/24/30 con
 * tercer dígito 5) para no usar identificaciones de personas reales (LOPDP, minimización).
 */
class EcuadorianIdValidatorTest {

    @Nested
    @DisplayName("Cédula (módulo 10)")
    class Cedula {

        @Test
        @DisplayName("acepta una cédula con verificador correcto: 2·2,0·1,5·2→1,9,9·2→9,... suma 59 → verificador 1")
        void acceptsValidCedula() {
            IdentificationResult result = EcuadorianIdValidator.validateCedula("2059999991");

            assertThat(result.valid()).isTrue();
            assertThat(result.checksumVerified()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"3059999981", "2459999971"})
        @DisplayName("acepta el código 30 (nacidos en el exterior) y la provincia 24")
        void acceptsAbroadAndLastProvince(String cedula) {
            assertThat(EcuadorianIdValidator.isValidCedula(cedula)).isTrue();
        }

        @Test
        void rejectsWrongCheckDigit() {
            IdentificationResult result = EcuadorianIdValidator.validateCedula("2059999992");

            assertThat(result.valid()).isFalse();
            assertThat(result.message()).contains("verificador");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0059999991", "2559999991", "9059999991"})
        @DisplayName("rechaza provincias 00, 25 y 90 (90 existe en la DPA pero no en cédulas)")
        void rejectsInvalidProvince(String cedula) {
            assertThat(EcuadorianIdValidator.validateCedula(cedula).message()).contains("provincia");
        }

        @Test
        void rejectsThirdDigitGreaterThanFive() {
            assertThat(EcuadorianIdValidator.validateCedula("2069999991").message()).contains("tercer dígito");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"205999999", "20599999911", "20599999a1", "205999999-", "２０５９９９９９９１"})
        @DisplayName("rechaza longitudes incorrectas y caracteres no ASCII")
        void rejectsMalformedInput(String cedula) {
            assertThat(EcuadorianIdValidator.isValidCedula(cedula)).isFalse();
        }
    }

    @Nested
    @DisplayName("RUC")
    class Ruc {

        @Test
        @DisplayName("persona natural: cédula válida + 001")
        void acceptsNaturalPersonRuc() {
            IdentificationResult result = EcuadorianIdValidator.validateRuc("2059999991001");

            assertThat(result.valid()).isTrue();
            assertThat(result.taxpayerType()).isEqualTo(TaxpayerType.NATURAL_PERSON);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2059999991002", "2059999992001"})
        @DisplayName("persona natural: rechaza establecimiento distinto de 001 o cédula inválida")
        void rejectsInvalidNaturalPersonRuc(String ruc) {
            assertThat(EcuadorianIdValidator.validateRuc(ruc).valid()).isFalse();
        }

        @Test
        @DisplayName("sociedad: reproduce el ejemplo del comunicado del SRI (suma 140, residuo 8, verificador 3)")
        void reproducesOfficialSriExample() {
            IdentificationResult result = EcuadorianIdValidator.validateRuc("1790085783001");

            assertThat(result.valid()).isTrue();
            assertThat(result.checksumVerified()).isTrue();
            assertThat(result.taxpayerType()).isEqualTo(TaxpayerType.PRIVATE_COMPANY);
        }

        @Test
        void acceptsSyntheticPrivateCompanyRuc() {
            assertThat(EcuadorianIdValidator.validateRuc("2099998701001").checksumVerified()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"1793189906001", "0993366721001", "2299981235001"})
        @DisplayName("sociedad: RUC con secuencial de 7 dígitos que no cumple módulo 11 sigue siendo válido (SRI, investigación #3)")
        void acceptsSevenDigitSequenceWithoutChecksum(String ruc) {
            IdentificationResult result = EcuadorianIdValidator.validateRuc(ruc);

            assertThat(result.valid()).isTrue();
            assertThat(result.checksumVerified()).isFalse();
            assertThat(result.message()).contains("consulta pública del SRI");
        }

        @ParameterizedTest
        @ValueSource(strings = {"2099998701002", "2599998701001", "3099998701001"})
        @DisplayName("sociedad: rechaza establecimiento distinto de 001 y provincias fuera de 01-24")
        void rejectsInvalidPrivateCompanyRuc(String ruc) {
            assertThat(EcuadorianIdValidator.validateRuc(ruc).valid()).isFalse();
        }

        @Test
        @DisplayName("entidad pública: verificador en la 9.ª posición con coeficientes 3,2,7,6,5,4,3,2")
        void acceptsPublicEntityRuc() {
            IdentificationResult result = EcuadorianIdValidator.validateRuc("2069999930001");

            assertThat(result.valid()).isTrue();
            assertThat(result.checksumVerified()).isTrue();
            assertThat(result.taxpayerType()).isEqualTo(TaxpayerType.PUBLIC_ENTITY);
        }

        @Test
        void flagsPublicEntityRucWithUnexpectedCheckDigit() {
            IdentificationResult result = EcuadorianIdValidator.validateRuc("2069999940001");

            assertThat(result.valid()).isTrue();
            assertThat(result.checksumVerified()).isFalse();
        }

        @Test
        void rejectsPublicEntityWithZeroEstablishment() {
            assertThat(EcuadorianIdValidator.validateRuc("2069999930000").valid()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"2079999930001", "2089999930001"})
        @DisplayName("rechaza tercer dígito 7 u 8")
        void rejectsUnknownTaxpayerType(String ruc) {
            assertThat(EcuadorianIdValidator.validateRuc(ruc).message()).contains("tercer dígito");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"205999999100", "20599999910011", "2059999991OO1"})
        void rejectsMalformedInput(String ruc) {
            assertThat(EcuadorianIdValidator.validateRuc(ruc).valid()).isFalse();
        }
    }
}
