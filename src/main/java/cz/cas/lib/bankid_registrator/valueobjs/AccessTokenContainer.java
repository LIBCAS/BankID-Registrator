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

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 *
 * @author iok
 */
@Component
public class AccessTokenContainer extends AccessTokenContainerAbstract {

    private final ConcurrentHashMap<String, String> codeTokenMap;

    public AccessTokenContainer() {
        this.codeTokenMap = new ConcurrentHashMap<>(0);
    }

    @Override
    public void Reset() {
        this.codeTokenMap.clear();
    }

    /**
     * 
     * @return 
     */
    @Override
    public synchronized ConcurrentHashMap<String, String> getCodeTokenMap() {
        return this.codeTokenMap;
    }

    /**
     * 
     * @param code
     * @return 
     */
    @Override
    public String getAccessToken(String code) {
        Assert.notNull(code, "\"code\" is required");

        return this.getCodeTokenMap().get(code);
    }

    /**
     * 
     * @param code
     * @param accessToken 
     * @return  
     */
    @Override
    public String setAccessToken(String code, String accessToken) {
        Assert.notNull(code, "\"code\" is required");
        Assert.notNull(accessToken, "\"accessToken\" is required");

        return this.codeTokenMap.putIfAbsent(code, accessToken);
    }

}
