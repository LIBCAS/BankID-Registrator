package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.token.IdentityToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityTokenRepository extends JpaRepository<IdentityToken, Long>
{
    Optional<IdentityToken> findByToken(String token);
}
