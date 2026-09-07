package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.ApiConfig;
import cz.cas.lib.bankid_registrator.configurations.RegistrationFeeConfig;
import cz.cas.lib.bankid_registrator.dao.mariadb.VoucherRepository;
import cz.cas.lib.bankid_registrator.dao.mariadb.VoucherUsageRepository;
import cz.cas.lib.bankid_registrator.entities.voucher.DiscountType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import cz.cas.lib.bankid_registrator.services.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiVoucherTest {
    @ParameterizedTest
    @CsvSource({"10,150,0", "16,280,130"})
    void previewUsesPatronTariff(String status, String fee, String remaining) {
        Identity identity = new Identity("test");
        identity.setId(1L);
        identity.setAlephId("TEST");
        Patron patron = new Patron();
        patron.setStatus(status);
        AlephService aleph = mock(AlephService.class);
        when(aleph.getAlephPatron("TEST", false)).thenReturn(Collections.singletonMap("patron", patron));
        IdentityService identities = mock(IdentityService.class);
        when(identities.findById(1L)).thenReturn(Optional.of(identity));
        MockHttpServletRequest request = new MockHttpServletRequest();
        IdentityAuthService auth = mock(IdentityAuthService.class);
        when(auth.isLoggedin(request)).thenReturn(true);
        when(auth.getAuthenticatedIdentityId(request)).thenReturn(1L);
        VoucherRepository repository = mock(VoucherRepository.class);
        when(repository.findByCode("TEST")).thenReturn(Optional.of(
            new Voucher("TEST", DiscountType.FIXED_CZK, new BigDecimal("150"), 1)));
        PatronService patrons = mock(PatronService.class);
        ApiController controller = new ApiController(mock(MessageSource.class), mock(ApiConfig.class),
            patrons, aleph, identities, mock(IdentityActivityService.class), mock(MapyCzService.class),
            mock(LdapService.class), mock(TokenService.class), auth,
            new VoucherService(repository, mock(VoucherUsageRepository.class)),
            new RegistrationFeeService(new RegistrationFeeConfig(), aleph, patrons));

        Map<String, Object> result = controller.validateVoucher("TEST", request).getBody();

        assertEquals(true, result.get("valid"));
        assertEquals(new BigDecimal(fee), result.get("feeAmount"));
        assertEquals(new BigDecimal("150"), result.get("discountAmount"));
        assertEquals(new BigDecimal(remaining), result.get("amountToPay"));
    }
}
