package cz.cas.lib.bankid_registrator.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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

    @NotBlank
    private String homeLibrary;

    @NotBlank
    private String admLibrary;

    @NotBlank
    private String bibLibrary;

    @NotNull
    private String[] libraries;

    @NotBlank
    private String sysno;

    @NotBlank
    private String itemBarcodePrefix;

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

    public String getHomeLibrary() {
        return homeLibrary;
    }

    public void setHomeLibrary(String homeLibrary) {
        this.homeLibrary = homeLibrary;
    }

    public String getAdmLibrary() {
        return admLibrary;
    }

    public void setAdmLibrary(String admLibrary) {
        this.admLibrary = admLibrary;
    }

    public String getBibLibrary() {
        return bibLibrary;
    }

    public void setBibLibrary(String bibLibrary) {
        this.bibLibrary = bibLibrary;
    }

    public String[] getLibraries() {
        return libraries;
    }

    public void setLibraries(String[] libraries) {
        this.libraries = libraries;
    }

    public String getSysno() {
        return sysno;
    }

    public void setSysno(String sysno) {
        this.sysno = sysno;
    }

    public String getItemBarcodePrefix() {
        return itemBarcodePrefix;
    }

    public void setItemBarcodePrefix(String itemBarcodePrefix) {
        this.itemBarcodePrefix = itemBarcodePrefix;
    }
}