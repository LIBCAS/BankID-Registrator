package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.LocaleConfig;
import cz.cas.lib.bankid_registrator.configurations.MessageConfig;
import cz.cas.lib.bankid_registrator.configurations.RegistrationFeeConfig;
import cz.cas.lib.bankid_registrator.configurations.VoucherIssuanceConfig;
import cz.cas.lib.bankid_registrator.entities.voucher.DiscountType;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherStatus;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherType;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import cz.cas.lib.bankid_registrator.services.AppSettingsService;
import cz.cas.lib.bankid_registrator.services.VoucherPrinterService;
import cz.cas.lib.bankid_registrator.services.VoucherService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoucherController.class)
@Import({ LocaleConfig.class, MessageConfig.class })
class VoucherControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VoucherService voucherService;

    @MockBean
    private AppSettingsService appSettingsService;

    @MockBean
    private VoucherPrinterService voucherPrinterService;

    @MockBean
    private RegistrationFeeConfig registrationFeeConfig;

    @MockBean
    private VoucherIssuanceConfig voucherIssuanceConfig;

    @Test
    @WithMockUser(username = "admin@example.org")
    void voucherListRendersSeparatedFiltersAndAllMatchingPrintAction() throws Exception {
        List<Voucher> vouchers = new ArrayList<>();
        for (long id = 1; id <= 25; id++) {
            Voucher voucher = new Voucher("CODE" + id, DiscountType.FIXED_CZK, new BigDecimal("100"), 1);
            voucher.setId(id);
            voucher.setCreatedAt(LocalDateTime.of(2026, 7, 24, 10, 0));
            voucher.setStatus(VoucherStatus.ACTIVE);
            voucher.setType(VoucherType.DIGITAL);
            vouchers.add(voucher);
        }

        when(voucherService.findVouchers(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(vouchers, PageRequest.of(0, 25), 26));
        when(appSettingsService.hasVoucherPrinterAppUrl()).thenReturn(true);

        mockMvc.perform(get("/dashboard/vouchers"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"voucherSearch\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"patronId\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"print-all-matching-vouchers\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"FILTERED\"")))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.containsString("id=\"selected-voucher-count\">0</span>")))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.containsString("id=\"voucher-page-number\">1 / 2</span>")))
                .andExpect(content()
                        .string(org.hamcrest.Matchers.containsString("id=\"voucher-display-range\">1 - 25</span>")))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("id=\"voucher-total-count\">26</span>")));
    }

    @Test
    void defaultExpiryUsesCalendarYearAnd2359IncludingLeapDay() {
        org.junit.jupiter.api.Assertions.assertEquals(LocalDateTime.of(2027, 7, 1, 23, 59),
                VoucherController.resolveGenerationExpiry("", false, java.time.LocalDate.of(2026, 7, 1)));
        org.junit.jupiter.api.Assertions.assertEquals(LocalDateTime.of(2025, 2, 28, 23, 59),
                VoucherController.resolveGenerationExpiry(null, false, java.time.LocalDate.of(2024, 2, 29)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "create,missing", "create,empty", "create,explicit", "create,never",
            "bulk-generate,missing", "bulk-generate,empty", "bulk-generate,explicit", "bulk-generate,never"
    })
    @WithMockUser(username = "admin@example.org")
    void generationResolvesExpiry(String method, String scenario) throws Exception {
        when(voucherIssuanceConfig.isAllowPartialDiscounts()).thenReturn(true);
        org.mockito.ArgumentCaptor<Voucher> saved = org.mockito.ArgumentCaptor.forClass(Voucher.class);
        org.mockito.ArgumentCaptor<LocalDateTime> bulkExpiry = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        if (method.equals("bulk-generate")) {
            when(voucherService.bulkGenerate(org.mockito.ArgumentMatchers.anyInt(), any(), any(),
                    org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.nullable(LocalDateTime.class),
                    any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        }
        java.time.LocalDate before = java.time.LocalDate.now();
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/dashboard/vouchers/" + method)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .param("code", "EXPIRYTEST").param("count", "2")
                .param("discountType", "PERCENTAGE").param("discountValue", "100");
        if (scenario.equals("empty"))
            request.param("expiresAt", "");
        if (scenario.equals("explicit") || scenario.equals("never"))
            request.param("expiresAt", "2029-08-12T15:30");
        if (scenario.equals("never"))
            request.param("neverExpires", "true");
        mockMvc.perform(request).andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attributeExists("successMessage"));
        LocalDateTime actual;
        if (method.equals("create")) {
            org.mockito.Mockito.verify(voucherService).save(saved.capture());
            actual = saved.getValue().getExpiresAt();
        } else {
            org.mockito.Mockito.verify(voucherService).bulkGenerate(org.mockito.ArgumentMatchers.eq(2), any(), any(),
                    org.mockito.ArgumentMatchers.eq(1), bulkExpiry.capture(), any(), any(), any(), any(), any(), any());
            actual = bulkExpiry.getValue();
        }
        if (scenario.equals("never"))
            org.junit.jupiter.api.Assertions.assertNull(actual);
        else if (scenario.equals("explicit"))
            org.junit.jupiter.api.Assertions.assertEquals(LocalDateTime.of(2029, 8, 12, 15, 30), actual);
        else
            org.junit.jupiter.api.Assertions.assertTrue(actual.equals(before.plusYears(1).atTime(23, 59))
                    || actual.equals(java.time.LocalDate.now().plusYears(1).atTime(23, 59)));
    }

    @Test
    @WithMockUser(username = "admin@example.org")
    void generationFormsRenderUncheckedNeverExpiryControls() throws Exception {
        mockMvc.perform(get("/dashboard/vouchers/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"create-neverExpires\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"bulk-neverExpires\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/assets/dist/js/admin.js")))
                .andExpect(content().string(org.hamcrest.Matchers
                        .not(org.hamcrest.Matchers.containsString("document.querySelectorAll('[data-expiry-input]')"))))
                .andExpect(
                        content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("checked="))));
    }

}
