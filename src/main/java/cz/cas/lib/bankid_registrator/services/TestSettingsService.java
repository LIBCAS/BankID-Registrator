package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.TestSettingsRepository;
import cz.cas.lib.bankid_registrator.model.test_settings.TestSettings;
import java.security.SecureRandom;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing test settings (Tester's Toolkit).
 * Only active in local and testing Spring profiles.
 */
@Service
@Profile({"local", "testing"})
public class TestSettingsService extends ServiceAbstract
{
    private static final long SINGLETON_ID = 1L;
    private static final String RANDOM_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";
    private static final int RANDOM_SUFFIX_LENGTH = 5;

    private final TestSettingsRepository testSettingsRepository;

    public TestSettingsService(TestSettingsRepository testSettingsRepository) {
        super(null);
        this.testSettingsRepository = testSettingsRepository;
    }

    /**
     * Get the singleton test settings row, creating it with defaults if missing.
     */
    @Transactional
    public TestSettings getSettings() {
        return this.testSettingsRepository.findById(SINGLETON_ID)
            .orElseGet(() -> this.testSettingsRepository.save(new TestSettings()));
    }

    /**
     * Get the current middle name override for fake BankID identities.
     */
    public String getMiddleName() {
        return getSettings().getMiddleName();
    }

    /**
     * Get whether renewal mode is force-enabled.
     */
    public boolean isForceRenewal() {
        return getSettings().isForceRenewal();
    }

    /**
     * Get the configured extra tester email for non-production email copies.
     */
    public String getTesterEmail() {
        return getSettings().getTesterEmail();
    }

    /**
     * Update the middle name override.
     */
    @Transactional
    public TestSettings updateMiddleName(String middleName) {
        TestSettings settings = getSettings();
        settings.setMiddleName(middleName);
        return this.testSettingsRepository.save(settings);
    }

    /**
     * Update the force renewal flag.
     */
    @Transactional
    public TestSettings updateForceRenewal(boolean forceRenewal) {
        TestSettings settings = getSettings();
        settings.setForceRenewal(forceRenewal);
        return this.testSettingsRepository.save(settings);
    }

    /**
     * Update the extra tester email.
     */
    @Transactional
    public TestSettings updateTesterEmail(String testerEmail) {
        TestSettings settings = getSettings();
        settings.setTesterEmail(testerEmail);
        return this.testSettingsRepository.save(settings);
    }

    /**
     * Update both settings at once.
     */
    @Transactional
    public TestSettings updateSettings(String middleName, String testerEmail, boolean forceRenewal) {
        TestSettings settings = getSettings();
        settings.setMiddleName(middleName);
        settings.setTesterEmail(testerEmail);
        settings.setForceRenewal(forceRenewal);
        return this.testSettingsRepository.save(settings);
    }

    /**
     * Generate a random middle name like "Test" + 5 random letters (e.g. "Testkbmrx").
     */
    public String generateRandomMiddleName() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("Test");
        for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
            char randomChar = RANDOM_CHARS.charAt(random.nextInt(RANDOM_CHARS.length()));
            sb.append(Character.toLowerCase(randomChar));
        }
        return sb.toString();
    }
}
