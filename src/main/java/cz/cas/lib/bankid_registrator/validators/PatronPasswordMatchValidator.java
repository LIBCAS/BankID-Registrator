package cz.cas.lib.bankid_registrator.validators;

import cz.cas.lib.bankid_registrator.dto.PatronPasswordDTO;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for checking if the new patron password and the repeated new patron password match
 */
public class PatronPasswordMatchValidator implements ConstraintValidator<PatronPasswordMatch, PatronPasswordDTO>
{
    @Override
    public void initialize(PatronPasswordMatch constraintAnnotation) {
    }

    @Override
    public boolean isValid(PatronPasswordDTO passwordDTO, ConstraintValidatorContext context) {
        boolean isValid = passwordDTO.getNewPassword() != null && 
                          passwordDTO.getNewPassword().equals(passwordDTO.getRepeatNewPassword());

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{form.patronPassword.error.notMatching}")
                   .addConstraintViolation();
        }

        return isValid;
    }
}