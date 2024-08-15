package cz.cas.lib.bankid_registrator.model.token;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Represents a blacklisted token
 */
@Entity
@Table(name = "tokens_blacklisted")
public class TokenBlacklisted
{
    @Id
    private String token;
    private LocalDateTime blacklistedAt;

    public TokenBlacklisted() {
    }

    public TokenBlacklisted(String token, LocalDateTime blacklistedAt) {
        this.token = token;
        this.blacklistedAt = blacklistedAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getBlacklistedAt() {
        return blacklistedAt;
    }

    public void setBlacklistedAt(LocalDateTime blacklistedAt) {
        this.blacklistedAt = blacklistedAt;
    }
}
