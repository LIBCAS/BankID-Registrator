package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.LdapServiceConfig;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.naming.directory.Attributes;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Service;
import org.springframework.context.MessageSource;

@Service
public class LdapService extends ServiceAbstract
{
    private final LdapServiceConfig ldapConfig;
    private LdapTemplate ldapTemplate;

    public LdapService(MessageSource messageSource, LdapServiceConfig ldapConfig) {
        super(messageSource);
        this.ldapConfig = ldapConfig;
    }

    @PostConstruct
    public void init()
    {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapConfig.getUrl());
        contextSource.setBase(ldapConfig.getBaseDn());
        if (ldapConfig.getUserDn() != null && !ldapConfig.getUserDn().isEmpty()) {
            contextSource.setUserDn(ldapConfig.getUserDn());
        }
        if (ldapConfig.getPassword() != null && !ldapConfig.getPassword().isEmpty()) {
            contextSource.setPassword(ldapConfig.getPassword());
        }
        contextSource.afterPropertiesSet();

        this.ldapTemplate = new LdapTemplate(contextSource);
    }

    /**
     * Check if an account with the given username exists in the LDAP
     * @param username Aleph patron barcode
     * @return
     */
    public boolean accountExistsByUsername(String username)
    {
        String searchFilter = "(eduPersonPrincipalName=" + username + ")";
        List<Attributes> result = ldapTemplate.search("", searchFilter, (Attributes attributes) -> attributes);

        return !result.isEmpty();
    }

    /**
     * Check if an account with the given email exists in the LDAP
     * @param email
     * @return
     */
    public boolean accountExistsByEmail(String email)
    {
        String searchFilter = "(mail=" + email + ")";
        List<Attributes> result = ldapTemplate.search("", searchFilter, (Attributes attributes) -> attributes);

        return !result.isEmpty();
    }

    /**
     * Check if an account with the given username and password exists in the LDAP
     * @param username Aleph patron ID, not Aleph patron barcode
     * @param password
     * @return
     */
    public boolean accountExistsByLogin(String username, String password)
    {
        try {
            password = password.toUpperCase();

            LdapContextSource contextSource = new LdapContextSource();
            contextSource.setUrl(ldapConfig.getUrl());
            contextSource.setBase(ldapConfig.getBaseDn());
            contextSource.setUserDn("uid=" + username + "," + ldapConfig.getBaseDn());
            contextSource.setPassword(password);
            contextSource.afterPropertiesSet();

            LdapTemplate tempLdapTemplate = new LdapTemplate(contextSource);
            tempLdapTemplate.authenticate("", "(uid=" + username + ")", password);

            return true;
        } catch (AuthenticationException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
