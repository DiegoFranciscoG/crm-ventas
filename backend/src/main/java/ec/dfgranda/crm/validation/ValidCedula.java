package ec.dfgranda.crm.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Cédula ecuatoriana válida (módulo 10). {@code null} o vacío se consideran válidos: el dato es opcional. */
@Documented
@Constraint(validatedBy = ValidCedula.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCedula {

    String message() default "Cédula inválida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidCedula, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isEmpty()) {
                return true;
            }
            IdentificationResult result = EcuadorianIdValidator.validateCedula(value);
            if (!result.valid()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(result.message()).addConstraintViolation();
            }
            return result.valid();
        }
    }
}
