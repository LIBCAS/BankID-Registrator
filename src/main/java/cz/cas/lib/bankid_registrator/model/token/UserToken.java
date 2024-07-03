package cz.cas.lib.bankid_registrator.model.token;

import cz.cas.lib.bankid_registrator.model.user.User;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_tokens")
@Getter
@Setter
public class UserToken extends BaseToken
{
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;
}
