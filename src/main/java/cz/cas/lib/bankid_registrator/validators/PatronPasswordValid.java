package cz.cas.lib.bankid_registrator.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PatronPasswordValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PatronPasswordValid {
    String message() default "{form.patronPassword.error.invalid}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
