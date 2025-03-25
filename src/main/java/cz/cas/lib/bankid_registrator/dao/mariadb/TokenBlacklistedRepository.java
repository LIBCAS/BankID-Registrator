package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.token.TokenBlacklisted;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenBlacklistedRepository extends JpaRepository<TokenBlacklisted, String>
{
    /**
     * Check if a token is blacklisted
     * @param token
     * @return
     */
    boolean existsByToken(String token);
}