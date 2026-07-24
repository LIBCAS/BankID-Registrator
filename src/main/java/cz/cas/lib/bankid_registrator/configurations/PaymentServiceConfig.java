package cz.cas.lib.bankid_registrator.configurations;

import org.checkerframework.checker.units.qual.N;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties("payment-service")
@Validated
public class PaymentServiceConfig {

    @NotBlank
    private String apiUrl;

    @NotBlank
    private String privateKeyPath;

    @Min(1)
    private int paymentDeadlineDays;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public int getPaymentDeadlineDays() {
        return paymentDeadlineDays;
    }

    public void setPaymentDeadlineDays(int paymentDeadlineDays) {
        this.paymentDeadlineDays = paymentDeadlineDays;
    }
}
