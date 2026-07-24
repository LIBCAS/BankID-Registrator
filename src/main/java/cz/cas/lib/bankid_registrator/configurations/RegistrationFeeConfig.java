package cz.cas.lib.bankid_registrator.configurations;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("registration-fee")
public class RegistrationFeeConfig {

    /**
     * Default registration/renewal fee amount in CZK.
     * Must match the fee amount configured in Aleph for the patron's item status.
     * Used for voucher discount calculation on the frontend before payment creation.
     */
    private BigDecimal defaultAmount = new BigDecimal("280");

    public BigDecimal getDefaultAmount() {
        return defaultAmount;
    }

    public void setDefaultAmount(BigDecimal defaultAmount) {
        this.defaultAmount = defaultAmount;
    }
}
