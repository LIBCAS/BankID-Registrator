package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.token.TokenBlacklisted;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TokenBlacklistedRepository extends JpaRepository<TokenBlacklisted, String>
{
    /**
     * Check if a token is blacklisted
     * @param token
     * @return
     */
    boolean existsByToken(String token);

    /**
     * Atomically blacklist a token. MariaDB returns 1 when inserted and 0 when
     * another request already consumed the same token.
     */
    @Modifying
    @Transactional
    @Query(
        value = "INSERT IGNORE INTO tokens_blacklisted (token, blacklistedAt) VALUES (:token, :blacklistedAt)",
        nativeQuery = true
    )
    int blacklistIfAbsent(
        @Param("token") String token,
        @Param("blacklistedAt") LocalDateTime blacklistedAt
    );

    /**
     * Idempotently release a token or submission claim.
     *
     * @return 1 when a row was deleted, otherwise 0
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TokenBlacklisted tokenBlacklisted WHERE tokenBlacklisted.token = :token")
    int deleteIfPresent(@Param("token") String token);
}
