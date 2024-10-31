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

import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.valueobjs.TokenContainer;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author iok
 */
public abstract class MainServiceAbstract implements ServiceIface {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 
     * @return 
     */
    protected final Logger getLogger() {
        return this.logger;
    }

    protected void init() {
        getLogger().debug("initializing...");
    }

    /**
     * 
     * @param issuer_url
     * @return 
     */
    public abstract URI getBankIDAuthorizationEndpoint(String issuer_url);

    /**
     * 
     * @param authorizationEndpoint
     * @return 
     */
    public abstract String getBankIDLoginURL(String authorizationEndpoint);

    /**
     * 
     * @param issuer_url
     * @return 
     */
    public abstract URI getBankIDTokenEndpoint(String issuer_url);

    /**
     * 
     * @param code
     * @throws Exception
     * @return 
     */
    public abstract TokenContainer getTokenExchange(String code) throws Exception;

    /**
     * 
     * @param issuer_url
     * @return 
     */
    public abstract URI getBankIDUserInfoEndpoint(String issuer_url);

    /**
     * 
     * @param access_token
     * @return 
     */
    public abstract Connect getUserInfo(String access_token);

    /**
     * 
     * @param issuer_url
     * @return 
     */
    public abstract URI getBankIDProfileEndpoint(String issuer_url);

    /**
     * 
     * @param access_token
     * @return 
     */
    public abstract Identify getProfile(String access_token);

}
