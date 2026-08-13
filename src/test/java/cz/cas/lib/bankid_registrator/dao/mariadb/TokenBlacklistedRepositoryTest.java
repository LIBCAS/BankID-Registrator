package cz.cas.lib.bankid_registrator.dao.mariadb;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class TokenBlacklistedRepositoryTest
{
    @Autowired
    private TokenBlacklistedRepository repository;

    @Test
    void blacklistIfAbsentUsesDeployedMariaDbColumnMapping() {
        String key = "TEST-SUBMISSION-CLAIM-" + UUID.randomUUID();

        try {
            assertEquals(1, repository.blacklistIfAbsent(key, LocalDateTime.now()));
            assertEquals(0, repository.blacklistIfAbsent(key, LocalDateTime.now()));
            assertEquals(1, repository.deleteIfPresent(key));
            assertEquals(0, repository.deleteIfPresent(key));
        } finally {
            repository.deleteIfPresent(key);
        }
    }
}
