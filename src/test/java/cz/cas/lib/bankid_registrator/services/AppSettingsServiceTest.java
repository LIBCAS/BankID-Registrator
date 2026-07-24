package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.AppSettingRepository;
import cz.cas.lib.bankid_registrator.dao.mariadb.SupportEmailRepository;
import cz.cas.lib.bankid_registrator.model.app_settings.AppSetting;
import cz.cas.lib.bankid_registrator.model.app_settings.SupportEmail;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppSettingsServiceTest
{
    @Test
    void replaceSupportEmailsDeletesExistingRowsBeforeSavingReplacementRows() {
        SupportEmailRepository supportEmailRepository = mock(SupportEmailRepository.class);
        AppSettingRepository appSettingRepository = mock(AppSettingRepository.class);
        when(supportEmailRepository.save(any(SupportEmail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppSettingsService appSettingsService = new AppSettingsService(supportEmailRepository, appSettingRepository);

        List<SupportEmail> saved = appSettingsService.replaceSupportEmails(
            Arrays.asList("help@knav.cz", "servis@knav.cz"),
            1
        );

        InOrder inOrder = inOrder(supportEmailRepository);
        inOrder.verify(supportEmailRepository).deleteAllInBatch();
        ArgumentCaptor<SupportEmail> savedEmails = ArgumentCaptor.forClass(SupportEmail.class);
        inOrder.verify(supportEmailRepository, times(2)).save(savedEmails.capture());

        assertEquals(2, saved.size());
        assertEquals("help@knav.cz", savedEmails.getAllValues().get(0).getEmail());
        assertEquals(0, savedEmails.getAllValues().get(0).getSortOrder());
        assertFalse(savedEmails.getAllValues().get(0).isPrimary());
        assertEquals("servis@knav.cz", savedEmails.getAllValues().get(1).getEmail());
        assertEquals(1, savedEmails.getAllValues().get(1).getSortOrder());
        assertTrue(savedEmails.getAllValues().get(1).isPrimary());
    }

    @Test
    void updateVoucherPrinterAppUrlSavesTrimmedAbsoluteHttpUrl() {
        SupportEmailRepository supportEmailRepository = mock(SupportEmailRepository.class);
        AppSettingRepository appSettingRepository = mock(AppSettingRepository.class);
        when(appSettingRepository.findById(AppSettingsService.VOUCHER_PRINTER_APP_URL_KEY)).thenReturn(Optional.empty());
        when(appSettingRepository.save(any(AppSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppSettingsService appSettingsService = new AppSettingsService(supportEmailRepository, appSettingRepository);

        Optional<String> savedUrl = appSettingsService.updateVoucherPrinterAppUrl(" https://printer.example.org/vouchers ");

        ArgumentCaptor<AppSetting> savedSetting = ArgumentCaptor.forClass(AppSetting.class);
        verify(appSettingRepository).save(savedSetting.capture());
        assertTrue(savedUrl.isPresent());
        assertEquals("https://printer.example.org/vouchers", savedUrl.get());
        assertEquals(AppSettingsService.VOUCHER_PRINTER_APP_URL_KEY, savedSetting.getValue().getKey());
        assertEquals("https://printer.example.org/vouchers", savedSetting.getValue().getValue());
    }

    @Test
    void updateVoucherPrinterAppUrlRejectsRelativeUrl() {
        SupportEmailRepository supportEmailRepository = mock(SupportEmailRepository.class);
        AppSettingRepository appSettingRepository = mock(AppSettingRepository.class);
        AppSettingsService appSettingsService = new AppSettingsService(supportEmailRepository, appSettingRepository);

        assertThrows(IllegalArgumentException.class, () -> appSettingsService.updateVoucherPrinterAppUrl("/voucher-printer"));
    }

    @Test
    void updateVoucherPrinterAppUrlDeletesSettingWhenBlank() {
        SupportEmailRepository supportEmailRepository = mock(SupportEmailRepository.class);
        AppSettingRepository appSettingRepository = mock(AppSettingRepository.class);
        AppSettingsService appSettingsService = new AppSettingsService(supportEmailRepository, appSettingRepository);

        Optional<String> savedUrl = appSettingsService.updateVoucherPrinterAppUrl(" ");

        assertFalse(savedUrl.isPresent());
        verify(appSettingRepository).deleteById(AppSettingsService.VOUCHER_PRINTER_APP_URL_KEY);
    }
}
