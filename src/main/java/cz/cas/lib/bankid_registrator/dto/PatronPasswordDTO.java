package cz.cas.lib.bankid_registrator.dto;

import cz.cas.lib.bankid_registrator.validators.PatronPasswordMatch;
import cz.cas.lib.bankid_registrator.validators.PatronPasswordValid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PatronPasswordMatch
public class PatronPasswordDTO
{
    @PatronPasswordValid
    private String newPassword;

    @PatronPasswordValid
    private String repeatNewPassword;
}
