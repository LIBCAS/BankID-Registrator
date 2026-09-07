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
     * Used as the standard tariff for voucher calculations before fee creation.
     */
    private BigDecimal defaultAmount = new BigDecimal("280");

    private BigDecimal seniorAmount = new BigDecimal("150");

    public BigDecimal getSeniorAmount() {
        return seniorAmount;
    }

    public void setSeniorAmount(BigDecimal seniorAmount) {
        this.seniorAmount = seniorAmount;
    }

    public BigDecimal getDefaultAmount() {
        return defaultAmount;
    }

    public void setDefaultAmount(BigDecimal defaultAmount) {
        this.defaultAmount = defaultAmount;
    }
}
