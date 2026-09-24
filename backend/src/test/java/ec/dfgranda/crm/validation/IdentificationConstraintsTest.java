package ec.dfgranda.crm.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IdentificationConstraintsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    record Form(@ValidRuc String ruc, @ValidCedula String cedula) {
    }

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void acceptsValidValuesAndOptionalCedula() {
        assertThat(validator.validate(new Form("2099998701001", null))).isEmpty();
        assertThat(validator.validate(new Form("2099998701001", ""))).isEmpty();
        assertThat(validator.validate(new Form("2099998701001", "2059999991"))).isEmpty();
    }

    @Test
    void reportsTheSpecificReasonAsMessage() {
        Set<ConstraintViolation<Form>> violations = validator.validate(new Form("2599998701001", "2059999992"));

        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "Código de provincia inválido: debe estar entre 01 y 24.",
                        "El dígito verificador de la cédula no es correcto.");
    }
}
