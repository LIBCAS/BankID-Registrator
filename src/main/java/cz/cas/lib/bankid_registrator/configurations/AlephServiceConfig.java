package cz.cas.lib.bankid_registrator.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties("aleph-service")
@Validated
public class AlephServiceConfig {

    @NotBlank
    private String host;

    @NotBlank
    private String port;

    @NotBlank
    private String restApiUri;

    @NotBlank
    private String wwwuser;

    @NotBlank
    private String wwwpasswd;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getRestApiUri() {
        return restApiUri;
    }

    public void setRestApiUri(String restApiUri) {
        this.restApiUri = restApiUri;
    }

    public String getWwwuser() {
        return wwwuser;
    }

    public void setWwwuser(String wwwuser) {
        this.wwwuser = wwwuser;
    }

    public String getWwwpasswd() {
        return wwwpasswd;
    }

    public void setWwwpasswd(String wwwpasswd) {
        this.wwwpasswd = wwwpasswd;
    }
}