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
package cz.cas.lib.bankid_registrator.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Display;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.Prompt;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.valueobjs.TokenContainer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 *
 * @author iok
 */
public class MainService extends MainServiceAbstract
{
    @Autowired
    MainConfiguration mainConfig;

    public MainService() {
        super();
        init();
    }

    /**
     * 
     * @param issuer_url
     * @return 
     */
    @Override
    public URI getBankIDAuthorizationEndpoint(String issuer_url) {

        Assert.notNull(issuer_url, "\"issuer_url\" is required");

        URI authorizationEndpoint = null;

        Issuer issuer = new Issuer(issuer_url);
            
        OIDCProviderConfigurationRequest request = new OIDCProviderConfigurationRequest(issuer);
            
        HTTPRequest httpRequest = request.toHTTPRequest();

        try {

            HTTPResponse httpResponse = httpRequest.send();

            OIDCProviderMetadata opMetadata = OIDCProviderMetadata.parse(httpResponse.getContentAsJSONObject());

            authorizationEndpoint = opMetadata.getAuthorizationEndpointURI();

        } catch (IOException | ParseException ex) {
            getLogger().error(MainService.class.getName(), ex);
        }

        return authorizationEndpoint;
    }

    /**
     * 
     * @param authorizationEndpoint
     * @return 
     */
    @Override
    public String getBankIDLoginURL(String authorizationEndpoint) {

        Assert.notNull(authorizationEndpoint, "\"authorizationEndpoint\" is required");

        Scope scope = new Scope();

        for (String key : this.mainConfig.getClient_scopes()) {
            // TODO check scope is released from BankID Configuration endpoint, before insert into object
            scope.add(key);
        }

        ClientID clientID = new ClientID(this.mainConfig.getClient_id());

        List<ACR> acrList = new ArrayList<>();
        acrList.add(new ACR("loa2"));

        AuthenticationRequest authRequest = null;

        ResponseType responseType = new ResponseType();
        responseType.add(ResponseType.Value.CODE);

        Prompt prompt = new Prompt();
        prompt.add(Prompt.Type.CONSENT);

        try {

            AuthenticationRequest.Builder authBuilder = new AuthenticationRequest.Builder(
                    responseType,
                    scope,
                    clientID,
                    new URI(this.mainConfig.getRedirect_url())
            );

            authBuilder.endpointURI(new URI(authorizationEndpoint));

            authBuilder.state(new State());

            authBuilder.nonce(new Nonce());

            authBuilder.prompt(prompt);

            authBuilder.display(Display.PAGE);

            authBuilder.acrValues(acrList);

            authBuilder.responseMode(ResponseMode.QUERY);

            authRequest = authBuilder.build();

            return authRequest.toQueryString();

        } catch (URISyntaxException ex) {
            getLogger().error(MainService.class.getName(), ex);
        }

        return null;
    }

    /**
     * 
     * @param issuer_url
     * @return 
     */
    @Override
    public URI getBankIDTokenEndpoint(String issuer_url) {

        Assert.notNull(issuer_url, "\"issuer_url\" is required");

        URI tokenEndpoint = null;

        Issuer issuer = new Issuer(issuer_url);
            
        OIDCProviderConfigurationRequest request = new OIDCProviderConfigurationRequest(issuer);
            
        HTTPRequest httpRequest = request.toHTTPRequest();

        try {            

            HTTPResponse httpResponse = httpRequest.send();

            OIDCProviderMetadata opMetadata = OIDCProviderMetadata.parse(httpResponse.getContentAsJSONObject());

            tokenEndpoint = opMetadata.getTokenEndpointURI();

        } catch (IOException | ParseException ex) {
            getLogger().error(MainService.class.getName(), ex);
        }

        return tokenEndpoint;
    }

    /**
     * 
     * @param code
     * @return 
     */
    @Override
    public TokenContainer getTokenExchange(String code) {

        Assert.notNull(code, "\"code\" is required");

        ClientID clientID = new ClientID(this.mainConfig.getClient_id());

        AuthorizationCode authorizationCode = new AuthorizationCode(code);

        URI tokenEndpointURI = this.getBankIDTokenEndpoint(this.mainConfig.getIssuer_url());

        TokenContainer tokenContainer = new TokenContainer();
        tokenContainer.Reset();

        try {

            URI callbackURI = new URI(this.mainConfig.getRedirect_url());

            AuthorizationGrant codeGrant = new AuthorizationCodeGrant(authorizationCode, callbackURI);

            Secret clientSecret = new Secret(this.mainConfig.getClient_secret());

            ClientAuthentication clientAuthentication = new ClientSecretPost(clientID, clientSecret);

            TokenRequest request = new TokenRequest(tokenEndpointURI, clientAuthentication, codeGrant);

            TokenResponse tokenResponse = OIDCTokenResponseParser.parse(request.toHTTPRequest().send());

            tokenContainer.setIndicateSuccess(tokenResponse.indicatesSuccess());

            if (tokenResponse.indicatesSuccess()) {

                StringBuilder accessToken = new StringBuilder(0);

                AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();

                accessToken.append(successResponse.getTokens().getAccessToken().getType().getValue().concat(" "));
                accessToken.append(successResponse.getTokens().getAccessToken().getValue());

                tokenContainer.setAccessToken(accessToken.toString());

                // settings in BankID
                if ((this.mainConfig.getUse_refresh_token()) && (successResponse.getTokens() != null)) {
                    tokenContainer.setRefreshToken(successResponse.getTokens().getRefreshToken().getValue());
                }

                tokenContainer.setIdToken(successResponse.getTokens().toOIDCTokens().getIDTokenString());

            } else {
                // TODO resolve error
                getLogger().error("Token endpoint failed: {}", tokenResponse.toErrorResponse().toJSONObject());
            }

        } catch (URISyntaxException | IOException | ParseException | NullPointerException ex) {
            getLogger().error(MainService.class.getName(), ex);
        }

       return tokenContainer;
    }

    /**
     * 
     * @param issuer_url
     * @return 
     */
    @Override
    public URI getBankIDUserInfoEndpoint(String issuer_url) {

        Assert.notNull(issuer_url, "\"issuer_url\" is required");

        URI userInfoEndpoint = null;

        Issuer issuer = new Issuer(issuer_url);
            
        OIDCProviderConfigurationRequest request = new OIDCProviderConfigurationRequest(issuer);
            
        HTTPRequest httpRequest = request.toHTTPRequest();

        try {            

            HTTPResponse httpResponse = httpRequest.send();

            OIDCProviderMetadata opMetadata = OIDCProviderMetadata.parse(httpResponse.getContentAsJSONObject());

            userInfoEndpoint = opMetadata.getUserInfoEndpointURI();

        } catch (IOException | ParseException ex) {
            getLogger().error(MainService.class.getName(), ex);
        }

        return userInfoEndpoint;
    }

    /**
     * 
     * @param accessToken
     * @return 
     */
    @Override
    public Connect getUserInfo(String accessToken) {

        Assert.notNull(accessToken, "\"accessToken\" is required");

        if (!accessToken.startsWith("Bearer")) {
            // TODO resolve error
        }

        URI userInfoEndpoint = this.getBankIDUserInfoEndpoint(this.mainConfig.getIssuer_url());

        Connect userInfo = null;

        try {

            BearerAccessToken token = BearerAccessToken.parse(accessToken);

            HTTPResponse dataResponse = new UserInfoRequest(userInfoEndpoint, token)
                    .toHTTPRequest()
                    .send();

            getLogger().debug("userInfo: {}", dataResponse.getContentAsJSONObject().toJSONString());

            ObjectMapper mapper = JsonMapper.builder()
                    .addModule(new JsonOrgModule())
                    .addModule(new JavaTimeModule()).build();
//                    .addModule(new JodaMoneyModule())
//                    .addModule(new JSONPModule()).build();

            userInfo = mapper.convertValue(dataResponse.getContentAsJSONObject(), Connect.class);

        } catch (ParseException | IOException ex) {
            getLogger().error(MainService.class.getName(), ex);
        }

        return userInfo;
    }

    /**
     * 
     * @param issuer_url
     * @return 
     */
    @Override
    public URI getBankIDProfileEndpoint(String issuer_url) {

        Assert.notNull(issuer_url, "\"issuer_url\" is required");

        if (issuer_url.endsWith("/")) {
            return URI.create(issuer_url.concat(this.mainConfig.getProfile_endpoint()));
        } else {
            return URI.create(issuer_url.concat("/").concat(this.mainConfig.getProfile_endpoint()));
        }
    }

    /**
     * 
     * @param accessToken
     * @return 
     */
    @Override
    public Identify getProfile(String accessToken) {

        Assert.notNull(accessToken, "\"accessToken\" is required");

        if (!accessToken.startsWith("Bearer")) {
            // TODO resolve error
        }

        Identify userProfile = null;

        URI endpoint = this.getBankIDProfileEndpoint(this.mainConfig.getIssuer_url());

        try {

            BearerAccessToken token = BearerAccessToken.parse(accessToken);

            HTTPResponse dataResponse = new UserInfoRequest(endpoint, token)
                    .toHTTPRequest()
                    .send();

            getLogger().debug("userProfile: {}", dataResponse.getContentAsJSONObject().toJSONString());

            ObjectMapper mapper = JsonMapper.builder()
                    .addModule(new JsonOrgModule())
                    .addModule(new JavaTimeModule()).build();
//                    .addModule(new JodaMoneyModule())
//                    .addModule(new JSONPModule()).build();

            userProfile = mapper.convertValue(dataResponse.getContentAsJSONObject(), Identify.class);

        } catch (ParseException | IOException ex) {
            getLogger().error(MainService.class.getName(), ex);
        }

        return userProfile;
    }

}
