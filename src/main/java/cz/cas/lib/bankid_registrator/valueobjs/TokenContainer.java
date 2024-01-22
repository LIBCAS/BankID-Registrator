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
package cz.cas.lib.bankid_registrator.valueobjs;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 *
 * @author iok
 */
@Component
public class TokenContainer extends TokenContainerAbstract implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String entityKey = UUID.randomUUID().toString();

    private boolean indicateSuccess = Boolean.FALSE;
    private String accessToken;
    private String refreshToken;
    private String idToken;

    public TokenContainer() {
        super();
        init();
    }

    @Override
    public synchronized void Reset() {
        this.indicateSuccess = Boolean.FALSE;
        this.accessToken = "";
        this.refreshToken = "";
        this.idToken = "";
    }

    /**
     * 
     * @return 
     */
    @Override
    public int hashCode() {
       int hash = 3;
       hash = 97 * hash + Objects.hashCode(this.entityKey);

       return hash;
    }

    /**
     * 
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return Boolean.TRUE;
        }
        if (obj == null) {
            return Boolean.FALSE;
        }
        if (getClass() != obj.getClass()) {
            return Boolean.FALSE;
        }
        final TokenContainer other = (TokenContainer) obj;

        return Objects.equals(this.entityKey, other.entityKey);
    }

    /**
     * 
     * @return 
     */
    public synchronized String getEntityKey() {
        return this.entityKey;
    }

    /**
     * 
     * @param indicateSuccess 
     */
    public synchronized void setIndicateSuccess(boolean indicateSuccess) {
        this.indicateSuccess = indicateSuccess;
    }

    /**
     * 
     * @return 
     */
    public synchronized boolean getIndicateSuccess() {
        return this.indicateSuccess;
    }

    /**
     * 
     * @param accessToken 
     */
    @Override
    public synchronized void setAccessToken(String accessToken) {
       Assert.notNull(accessToken, "\"accessToken\" is required");
       this.accessToken = accessToken;
    }

    /**
     * 
     * @return 
     */
    @Override
    public synchronized String getAccessToken() {
        return this.accessToken;
    }

    /**
     * 
     * @param refreshToken 
     */
    @Override
    public synchronized void setRefreshToken(String refreshToken) {
        Assert.notNull(refreshToken, "\"refreshToken\" is required");
        this.refreshToken = refreshToken;
    }

    /**
     * 
     * @return 
     */
    @Override
    public synchronized String getRefreshToken() {
        return this.refreshToken;
    }

    /**
     * 
     * @param idToken 
     */
    @Override
    public synchronized void setIdToken(String idToken) {
        Assert.notNull(idToken, "\"idToken\" is required");
        this.idToken = idToken;
    }

    /**
     * 
     * @return 
     */
    @Override
    public synchronized String getIdToken() {
        return this.idToken;
    }

}
