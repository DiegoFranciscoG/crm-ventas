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

/** RUC ecuatoriano con estructura válida. {@code null} se considera válido (combinar con {@code @NotBlank}). */
@Documented
@Constraint(validatedBy = ValidRuc.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRuc {

    String message() default "RUC inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidRuc, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            IdentificationResult result = EcuadorianIdValidator.validateRuc(value);
            if (!result.valid()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(result.message()).addConstraintViolation();
            }
            return result.valid();
        }
    }
}
