package cz.cas.lib.bankid_registrator.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "token-service")
@Validated
public class TokenServiceConfig
{
    private long identityTokenExpiration = 86400000;    // Default is 24 hours
    private long userTokenExpiration = 86400000;    // Default is 24 hours
    private long apiTokenExpiration = 900000;   // Default is 15 minutes

    public long getIdentityTokenExpiration() {
        return identityTokenExpiration;
    }

    public void setIdentityTokenExpiration(long identityTokenExpiration) {
        this.identityTokenExpiration = identityTokenExpiration;
    }

    public long getUserTokenExpiration() {
        return userTokenExpiration;
    }

    public void setUserTokenExpiration(long userTokenExpiration) {
        this.userTokenExpiration = userTokenExpiration;
    }

    public long getApiTokenExpiration() {
        return apiTokenExpiration;
    }

    public void setApiTokenExpiration(long apiTokenExpiration) {
        this.apiTokenExpiration = apiTokenExpiration;
    }
}
