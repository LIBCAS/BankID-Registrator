package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.TokenServiceConfig;
import cz.cas.lib.bankid_registrator.dao.mariadb.TokenBlacklistedRepository;
import cz.cas.lib.bankid_registrator.util.JwtUtil;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceTest
{
    @Test
    void tryInvalidateTokenClaimsTokenOnlyWhenRepositoryInsertsIt() {
        TokenBlacklistedRepository repository = mock(TokenBlacklistedRepository.class);
        TokenService service = tokenService(repository);
        when(repository.blacklistIfAbsent(eq("one-time-token"), any(LocalDateTime.class)))
            .thenReturn(1, 0);

        assertTrue(service.tryInvalidateToken("one-time-token"));
        assertFalse(service.tryInvalidateToken("one-time-token"));
    }

    @Test
    void reactivateTokenRemovesFailedClaim() {
        TokenBlacklistedRepository repository = mock(TokenBlacklistedRepository.class);
        TokenService service = tokenService(repository);

        service.reactivateToken("retryable-token");

        verify(repository).deleteIfPresent("retryable-token");
    }

    @Test
    void arbitraryBusinessKeyUsesSameAtomicClaimStorage() {
        TokenBlacklistedRepository repository = mock(TokenBlacklistedRepository.class);
        TokenService service = tokenService(repository);
        String key = "submission:membership-renewal:42:27/07/2026";
        when(repository.blacklistIfAbsent(eq(key), any(LocalDateTime.class))).thenReturn(1);

        assertTrue(service.tryClaimKey(key));
        service.releaseClaimKey(key);

        verify(repository).deleteIfPresent(key);
    }

    private TokenService tokenService(TokenBlacklistedRepository repository) {
        return new TokenService(
            mock(JwtUtil.class),
            mock(TokenServiceConfig.class),
            repository
        );
    }
}
