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
@Import({LocaleConfig.class, MessageConfig.class})
class VoucherControllerTest
{
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
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(new PageImpl<>(vouchers, PageRequest.of(0, 25), 26));
        when(appSettingsService.hasVoucherPrinterAppUrl()).thenReturn(true);

        mockMvc.perform(get("/dashboard/vouchers"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"voucherSearch\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"patronId\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"print-all-matching-vouchers\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"FILTERED\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"selected-voucher-count\">0</span>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"voucher-page-number\">1 / 2</span>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"voucher-display-range\">1 - 25</span>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"voucher-total-count\">26</span>")));
    }
}
