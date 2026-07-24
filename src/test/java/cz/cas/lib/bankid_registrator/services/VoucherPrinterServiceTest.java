package cz.cas.lib.bankid_registrator.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.bankid_registrator.configurations.RegistrationFeeConfig;
import cz.cas.lib.bankid_registrator.dto.VoucherPrintData;
import cz.cas.lib.bankid_registrator.entities.voucher.DiscountType;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherRecipientType;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VoucherPrinterServiceTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildPrinterUrlUsesRealVoucherDataAndComputedCappedCzkDiscount() {
        AppSettingsService appSettingsService = mock(AppSettingsService.class);
        VoucherService voucherService = mock(VoucherService.class);
        RegistrationFeeConfig registrationFeeConfig = new RegistrationFeeConfig();
        registrationFeeConfig.setDefaultAmount(new BigDecimal("280"));
        VoucherPrinterService voucherPrinterService = new VoucherPrinterService(appSettingsService, registrationFeeConfig, voucherService, objectMapper);

        Voucher voucher = new Voucher("FREE100", DiscountType.PERCENTAGE, new BigDecimal("100"), 1);
        voucher.setCreatedAt(LocalDateTime.of(2026, 6, 29, 9, 45, 24));
        voucher.setExpiresAt(LocalDateTime.of(2027, 6, 29, 9, 45, 24));
        voucher.setRecipientType(VoucherRecipientType.SENIOR);

        when(appSettingsService.getVoucherPrinterAppUrl()).thenReturn(Optional.of("https://printer.example.org/"));
        when(voucherService.computeDiscount(voucher, new BigDecimal("280"))).thenReturn(new BigDecimal("280.00"));

        Optional<String> printerUrl = voucherPrinterService.buildPrinterUrl(voucher);

        assertTrue(printerUrl.isPresent());
        assertTrue(printerUrl.get().startsWith("https://printer.example.org/?"));
        assertTrue(printerUrl.get().contains("code=FREE100"));
        assertTrue(printerUrl.get().contains("issuedat=2026-06-29%2009:45:24"));
        assertTrue(printerUrl.get().contains("expiresat=2027-06-29%2009:45:24"));
        assertTrue(printerUrl.get().contains("discount=280"));
        assertTrue(printerUrl.get().contains("recipient_type=SENIOR"));
    }

    @Test
    void buildBatchPayloadUsesVoucherPrinterShape() throws Exception {
        AppSettingsService appSettingsService = mock(AppSettingsService.class);
        VoucherService voucherService = mock(VoucherService.class);
        RegistrationFeeConfig registrationFeeConfig = new RegistrationFeeConfig();
        registrationFeeConfig.setDefaultAmount(new BigDecimal("280"));
        VoucherPrinterService voucherPrinterService = new VoucherPrinterService(appSettingsService, registrationFeeConfig, voucherService, objectMapper);

        Voucher standardVoucher = new Voucher("CODE001", DiscountType.FIXED_CZK, new BigDecimal("280"), 1);
        standardVoucher.setCreatedAt(LocalDateTime.of(2026, 6, 29, 9, 45, 24));
        standardVoucher.setExpiresAt(LocalDateTime.of(2027, 6, 29, 9, 45, 24));

        Voucher seniorVoucher = new Voucher("CODE002", DiscountType.FIXED_CZK, new BigDecimal("150"), 1);
        seniorVoucher.setCreatedAt(LocalDateTime.of(2026, 7, 29, 9, 45, 24));
        seniorVoucher.setRecipientType(VoucherRecipientType.SENIOR);

        when(voucherService.computeDiscount(standardVoucher, new BigDecimal("280"))).thenReturn(new BigDecimal("280.00"));
        when(voucherService.computeDiscount(seniorVoucher, new BigDecimal("280"))).thenReturn(new BigDecimal("150.00"));

        JsonNode payload = objectMapper.readTree(voucherPrinterService.buildBatchPayload(Arrays.asList(standardVoucher, seniorVoucher)));

        assertEquals(2, payload.get("vouchers").size());
        assertEquals("CODE001", payload.get("vouchers").get(0).get("code").asText());
        assertEquals("2026-06-29 09:45:24", payload.get("vouchers").get(0).get("issuedat").asText());
        assertEquals("2027-06-29 09:45:24", payload.get("vouchers").get(0).get("expiresat").asText());
        assertEquals(280, payload.get("vouchers").get(0).get("discount").asInt());
        assertEquals("", payload.get("vouchers").get(0).get("recipient_type").asText());
        assertEquals("SENIOR", payload.get("vouchers").get(1).get("recipient_type").asText());
    }

    @Test
    void buildBatchPayloadFromPrintDataUsesLightweightProjection() throws Exception {
        AppSettingsService appSettingsService = mock(AppSettingsService.class);
        VoucherService voucherService = mock(VoucherService.class);
        RegistrationFeeConfig registrationFeeConfig = new RegistrationFeeConfig();
        registrationFeeConfig.setDefaultAmount(new BigDecimal("280"));
        VoucherPrinterService voucherPrinterService = new VoucherPrinterService(
            appSettingsService,
            registrationFeeConfig,
            voucherService,
            objectMapper
        );
        VoucherPrintData printData = new VoucherPrintData(
            42L,
            "FILTERED001",
            LocalDateTime.of(2026, 7, 24, 10, 15, 0),
            null,
            null,
            DiscountType.PERCENTAGE,
            new BigDecimal("50"),
            VoucherRecipientType.SENIOR
        );
        when(voucherService.computeDiscount(
            DiscountType.PERCENTAGE,
            new BigDecimal("50"),
            new BigDecimal("280")
        )).thenReturn(new BigDecimal("140.00"));

        JsonNode payload = objectMapper.readTree(
            voucherPrinterService.buildBatchPayloadFromPrintData(Arrays.asList(printData))
        );

        assertEquals(1, payload.get("vouchers").size());
        assertEquals("FILTERED001", payload.get("vouchers").get(0).get("code").asText());
        assertEquals("2026-07-24 10:15:00", payload.get("vouchers").get(0).get("issuedat").asText());
        assertEquals("", payload.get("vouchers").get(0).get("expiresat").asText());
        assertEquals(140, payload.get("vouchers").get(0).get("discount").asInt());
        assertEquals("SENIOR", payload.get("vouchers").get(0).get("recipient_type").asText());
    }

    @Test
    void buildPrinterUrlReturnsEmptyWhenVoucherPrinterIsNotConfigured() {
        AppSettingsService appSettingsService = mock(AppSettingsService.class);
        VoucherService voucherService = mock(VoucherService.class);
        RegistrationFeeConfig registrationFeeConfig = new RegistrationFeeConfig();
        VoucherPrinterService voucherPrinterService = new VoucherPrinterService(appSettingsService, registrationFeeConfig, voucherService, objectMapper);

        when(appSettingsService.getVoucherPrinterAppUrl()).thenReturn(Optional.empty());

        assertFalse(voucherPrinterService.buildPrinterUrl(new Voucher()).isPresent());
    }
}
