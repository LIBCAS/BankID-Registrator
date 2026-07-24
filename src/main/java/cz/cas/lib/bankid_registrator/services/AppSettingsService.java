package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.AppSettingRepository;
import cz.cas.lib.bankid_registrator.dao.mariadb.SupportEmailRepository;
import cz.cas.lib.bankid_registrator.model.app_settings.AppSetting;
import cz.cas.lib.bankid_registrator.model.app_settings.SupportEmail;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppSettingsService
{
    public static final String VOUCHER_PRINTER_APP_URL_KEY = "voucher_printer_app_url";

    private final SupportEmailRepository supportEmailRepository;
    private final AppSettingRepository appSettingRepository;

    public AppSettingsService(SupportEmailRepository supportEmailRepository, AppSettingRepository appSettingRepository) {
        this.supportEmailRepository = supportEmailRepository;
        this.appSettingRepository = appSettingRepository;
    }

    public List<SupportEmail> getSupportEmails() {
        return this.supportEmailRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    public Optional<SupportEmail> getPrimarySupportEmail() {
        return this.supportEmailRepository.findByPrimaryTrue();
    }

    public Optional<String> getVoucherPrinterAppUrl() {
        return this.appSettingRepository.findById(VOUCHER_PRINTER_APP_URL_KEY)
            .map(AppSetting::getValue)
            .map(String::trim)
            .filter(value -> !value.isEmpty());
    }

    public boolean hasVoucherPrinterAppUrl() {
        return this.getVoucherPrinterAppUrl().isPresent();
    }

    @Transactional
    public Optional<String> updateVoucherPrinterAppUrl(String rawUrl) {
        String normalizedUrl = normalizeVoucherPrinterAppUrl(rawUrl);
        if (normalizedUrl == null) {
            this.appSettingRepository.deleteById(VOUCHER_PRINTER_APP_URL_KEY);
            return Optional.empty();
        }

        AppSetting setting = this.appSettingRepository.findById(VOUCHER_PRINTER_APP_URL_KEY).orElseGet(AppSetting::new);
        setting.setKey(VOUCHER_PRINTER_APP_URL_KEY);
        setting.setValue(normalizedUrl);
        this.appSettingRepository.save(setting);
        return Optional.of(normalizedUrl);
    }

    @Transactional
    public List<SupportEmail> replaceSupportEmails(List<String> rawEmails, Integer primaryIndex) {
        List<String> emails = normalizeEmails(rawEmails);
        String primaryEmail = getSelectedPrimaryEmail(rawEmails, primaryIndex);
        if (!emails.isEmpty() && (primaryEmail == null || !emails.contains(primaryEmail))) {
            throw new IllegalArgumentException("A primary support email must be selected.");
        }

        this.supportEmailRepository.deleteAllInBatch();
        List<SupportEmail> saved = new ArrayList<>();
        for (int i = 0; i < emails.size(); i++) {
            SupportEmail supportEmail = new SupportEmail();
            supportEmail.setEmail(emails.get(i));
            supportEmail.setSortOrder(i);
            supportEmail.setPrimary(emails.get(i).equals(primaryEmail));
            saved.add(this.supportEmailRepository.save(supportEmail));
        }
        return saved;
    }

    private String getSelectedPrimaryEmail(List<String> rawEmails, Integer primaryIndex) {
        if (rawEmails == null || primaryIndex == null || primaryIndex < 0 || primaryIndex >= rawEmails.size()) {
            return null;
        }
        String selected = rawEmails.get(primaryIndex);
        return selected == null || selected.trim().isEmpty() ? null : selected.trim();
    }

    private List<String> normalizeEmails(List<String> rawEmails) {
        Set<String> uniqueEmails = new LinkedHashSet<>();
        if (rawEmails == null) {
            return new ArrayList<>();
        }
        for (String rawEmail : rawEmails) {
            String email = rawEmail == null ? "" : rawEmail.trim();
            if (email.isEmpty()) {
                continue;
            }
            if (!isValidEmail(email)) {
                throw new IllegalArgumentException("Invalid support email: " + email);
            }
            uniqueEmails.add(email);
        }
        return new ArrayList<>(uniqueEmails);
    }

    private boolean isValidEmail(String email) {
        try {
            InternetAddress address = new InternetAddress(email);
            address.validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }

    private String normalizeVoucherPrinterAppUrl(String rawUrl) {
        String url = rawUrl == null ? "" : rawUrl.trim();
        if (url.isEmpty()) {
            return null;
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("Voucher printer app URL must be an absolute HTTP(S) URL.");
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Voucher printer app URL.", e);
        }
    }
}
