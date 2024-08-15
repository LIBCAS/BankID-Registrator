package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.LdapServiceConfig;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.naming.directory.Attributes;
import java.util.List;

@Service
public class LdapService
{
    private final LdapServiceConfig ldapConfig;
    private LdapTemplate ldapTemplate;

    public LdapService(LdapServiceConfig ldapConfig) {
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
}
