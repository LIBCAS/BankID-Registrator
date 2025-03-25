package cz.cas.lib.bankid_registrator.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validator for Patron password - when setting a new password after registration or when changing the password
 */
public class PatronPasswordValidator implements ConstraintValidator<PatronPasswordValid, String>
{
    // A password of 6-13 characters containing at least one letter and one number. Only letters, numbers and special characters are allowed
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d\\W]{6,13}$");

    @Override
    public void initialize(PatronPasswordValid constraintAnnotation) {
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{form.patronPassword.error.invalid}")
                   .addConstraintViolation();
            return false;
        }
        return true;
    }
}