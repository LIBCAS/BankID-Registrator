package cz.cas.lib.bankid_registrator.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PatronPasswordMatchValidator.class)
@Documented
public @interface PatronPasswordMatch {
    String message() default "{form.patronPassword.error.notMatching}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
