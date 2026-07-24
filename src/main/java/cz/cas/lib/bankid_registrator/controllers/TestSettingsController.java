package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.model.test_settings.TestSettings;
import cz.cas.lib.bankid_registrator.services.TestSettingsService;
import java.util.HashMap;
import java.util.Map;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for the Tester's Toolkit.
 * Only active in local and testing Spring profiles.
 */
@RestController
@RequestMapping("/api/test-settings")
@Profile({"local", "testing"})
public class TestSettingsController
{
    private final TestSettingsService testSettingsService;

    public TestSettingsController(TestSettingsService testSettingsService) {
        this.testSettingsService = testSettingsService;
    }

    /**
     * Get current test settings.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings()
    {
        TestSettings settings = this.testSettingsService.getSettings();
        Map<String, Object> result = new HashMap<>();
        result.put("middleName", settings.getMiddleName());
        result.put("testerEmail", settings.getTesterEmail());
        result.put("forceRenewal", settings.isForceRenewal());
        result.put("updatedAt", settings.getUpdatedAt() != null ? settings.getUpdatedAt().toString() : null);
        return ResponseEntity.ok(result);
    }

    /**
     * Update test settings.
     * Accepts optional parameters — only provided ones are updated.
     */
    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(
        @RequestParam(value = "middleName", required = false) String middleName,
        @RequestParam(value = "testerEmail", required = false) String testerEmail,
        @RequestParam(value = "forceRenewal", required = false) Boolean forceRenewal
    ) {
        TestSettings settings = this.testSettingsService.getSettings();

        if (middleName != null) {
            String trimmed = middleName.trim();
            if (trimmed.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "middleName cannot be empty");
                return ResponseEntity.badRequest().body(error);
            }
            settings.setMiddleName(trimmed);
        }

        if (testerEmail != null) {
            String trimmed = testerEmail.trim();
            if (!trimmed.isEmpty() && !isValidEmail(trimmed)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "testerEmail is invalid");
                return ResponseEntity.badRequest().body(error);
            }
            settings.setTesterEmail(trimmed.isEmpty() ? null : trimmed);
        }

        if (forceRenewal != null) {
            settings.setForceRenewal(forceRenewal);
        }

        settings = this.testSettingsService.updateSettings(
            settings.getMiddleName(),
            settings.getTesterEmail(),
            settings.isForceRenewal()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("middleName", settings.getMiddleName());
        result.put("testerEmail", settings.getTesterEmail());
        result.put("forceRenewal", settings.isForceRenewal());
        result.put("updatedAt", settings.getUpdatedAt() != null ? settings.getUpdatedAt().toString() : null);
        return ResponseEntity.ok(result);
    }

    /**
     * Generate a random middle name and return it (does NOT save it).
     */
    @GetMapping("/generate-middle-name")
    public ResponseEntity<Map<String, Object>> generateMiddleName()
    {
        String generated = this.testSettingsService.generateRandomMiddleName();
        Map<String, Object> result = new HashMap<>();
        result.put("middleName", generated);
        return ResponseEntity.ok(result);
    }

    private boolean isValidEmail(String email)
    {
        try {
            InternetAddress address = new InternetAddress(email);
            address.validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }
}
