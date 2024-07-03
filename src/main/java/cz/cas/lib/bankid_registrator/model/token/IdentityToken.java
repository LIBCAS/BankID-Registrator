package cz.cas.lib.bankid_registrator.model.token;

import cz.cas.lib.bankid_registrator.model.identity.Identity;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "identity_tokens")
@Getter
@Setter
public class IdentityToken extends BaseToken
{
    @ManyToOne
    @JoinColumn(name = "identity_id", nullable = false, unique = true, updatable = false)
    private Identity identity;
}
