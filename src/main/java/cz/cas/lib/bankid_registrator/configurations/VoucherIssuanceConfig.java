package cz.cas.lib.bankid_registrator.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("voucher")
public class VoucherIssuanceConfig {

    /**
     * Whether admins may issue vouchers that cover less than the full configured
     * registration/renewal fee. This affects issuance only; already-issued vouchers
     * remain valid regardless of later configuration changes.
     */
    private boolean allowPartialDiscounts = true;

    public boolean isAllowPartialDiscounts() {
        return allowPartialDiscounts;
    }

    public void setAllowPartialDiscounts(boolean allowPartialDiscounts) {
        this.allowPartialDiscounts = allowPartialDiscounts;
    }
}
