/*
 * Copyright (C) 2022 Academy of Sciences Library
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.bankid_registrator.configurations;

import cz.cas.lib.bankid_registrator.util.DateUtils;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author iok
 */
@Configuration
@ConfigurationProperties("bankid-registrator")
public class MainConfiguration extends ConfigurationAbstract {

    private final String propertyPrefix = "bankid-registrator";

    @NotBlank
    private String client_id;

    @NotBlank
    private String client_secret;

    @NotBlank
    private String issuer_url;

    @NotBlank
    private String profile_endpoint;

    @NotBlank
    private String redirect_url;

    @NotBlank
    @Value("#{'${bankid-registrator.client_scopes}'.split(',')}")
    private List<String> client_scopes;

    @NotNull
    @Value("#{new Boolean('${bankid-registrator.use_refresh_token}')}")
    private boolean use_refresh_token;

    @NotNull
    @Value("#{new Boolean('bankid-registrator.rewrite_aleph_batch_xml_header')}")
    private boolean rewrite_aleph_batch_xml_header;

    @NotBlank
    private String id_prefix;

    @NotBlank
    private String barcode_prefix;

    @NotBlank
    @Pattern(regexp="^[0-9]{1,}$")
    private String length_of_registration;

    public MainConfiguration() {
        super();
        init();
    }

    /**
     * 
     * @param client_id 
     */
    public synchronized void setClient_id(String client_id) {
        this.client_id = client_id;
        getLogger().debug("Set {}.client_id={}", this.propertyPrefix, this.client_id);
    }

    /**
     * 
     * @return 
     */
    public synchronized String getClient_id() {
        return this.client_id;
    }

    /**
     * 
     * @param client_secret 
     */
    public synchronized void setClient_secret(String client_secret) {
        this.client_secret = client_secret;
        getLogger().debug("Set {}.client_secret={}", this.propertyPrefix, this.client_secret);
    }

    /**
     * 
     * @return 
     */
    public synchronized String getClient_secret() {
        return this.client_secret;
    }

    /**
     * 
     * @param client_scopes 
     */
    public synchronized void setClient_scopes(List<String> client_scopes) {
        this.client_scopes = client_scopes;
        getLogger().debug("Set {}.client_scopes={}", this.propertyPrefix, this.client_scopes.toString());
    }

    /**
     * 
     * @return 
     */
    public synchronized List<String> getClient_scopes() {
        return this.client_scopes;
    }

    /**
     * 
     * @param issuer_url 
     */
    public synchronized void setIssuer_url(String issuer_url) {
        this.issuer_url = issuer_url;
        getLogger().debug("Set {}.issuer_url={}", this.propertyPrefix, this.issuer_url);
    }

    /**
     * 
     * @return 
     */
    public synchronized String getIssuer_url() {
        return this.issuer_url;
    }

    /**
     * 
     * @param profile_endpoint 
     */
    public synchronized void setProfile_endpoint(String profile_endpoint) {
        this.profile_endpoint = profile_endpoint;
        getLogger().debug("Set {}.profile_endpoint={}", this.propertyPrefix, this.profile_endpoint);
    }

    /**
     * 
     * @return 
     */
    public synchronized String getProfile_endpoint() {
        return this.profile_endpoint;
    }

    /**
     * 
     * @param redirect_url 
     */
    public synchronized void setRedirect_url(String redirect_url) {
        this.redirect_url = redirect_url;
        getLogger().debug("Set {}.redirect_url={}", this.propertyPrefix, this.redirect_url);
    }

    /**
     * 
     * @return 
     */
    public synchronized String getRedirect_url() {
        return this.redirect_url;
    }

    /**
     * 
     * @param use_refresh_token 
     */
    public synchronized void setUse_refresh_token(boolean use_refresh_token) {
        this.use_refresh_token = use_refresh_token;
        getLogger().debug("Set {}.use_refresh_token={}", this.propertyPrefix, this.use_refresh_token);
    }

    /**
     * 
     * @return 
     */
    public synchronized boolean getUse_refresh_token() {
        return this.use_refresh_token;
    }

    /**
     * 
     * @param rewrite_aleph_batch_xml_header 
     */
    public synchronized void setRewrite_aleph_batch_xml_header(boolean rewrite_aleph_batch_xml_header) {
        this.rewrite_aleph_batch_xml_header = rewrite_aleph_batch_xml_header;
        getLogger().debug("Set {}.rewrite_aleph_batch_xml_header={}", this.propertyPrefix, this.rewrite_aleph_batch_xml_header);
    }

    /**
     * 
     * @return 
     */
    public synchronized boolean getRewrite_aleph_batch_xml_header() {
        return this.rewrite_aleph_batch_xml_header;
    }

    /**
     * 
     * @param id_prefix 
     */
    public synchronized void setId_prefix(String id_prefix) {
        this.id_prefix = id_prefix;
        getLogger().debug("Set {}.id_prefix={}", this.propertyPrefix, this.id_prefix);
    }

    /**
     * 
     * @return 
     */
    public synchronized String getId_prefix() {
        return this.id_prefix;
    }

    /**
     * 
     * @param barcode_prefix 
     */
    public synchronized void setBarcode_prefix(String barcode_prefix) {
        this.barcode_prefix = barcode_prefix;
        getLogger().debug("Set {}.barcode_prefix={}", this.propertyPrefix, this.barcode_prefix);
    }

    /**
     * 
     * @return 
     */
    public synchronized String getBarcode_prefix() {
        return this.barcode_prefix;
    }

    /**
     * 
     * @param length_of_registration 
     */
    public synchronized void setLength_of_registration(String length_of_registration) {
        this.length_of_registration = length_of_registration;
        getLogger().debug("Set {}.length_of_registration={}", this.propertyPrefix, this.length_of_registration);
    }

    /**
     * 
     * @return 
     */
    public synchronized String getLength_of_registration() {
        return this.length_of_registration;
    }


    @Bean
    public DateUtils dateUtils() {
        return new DateUtils();
    }
}
