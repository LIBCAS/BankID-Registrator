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
package cz.cas.lib.bankid_registrator.entities.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author iok
 */
public class Verification {

    private final TrustFramework trust_framework;
    private final String time;
    private final String verification_process;

    /**
     * 
     * @param trust_framework
     * @param time
     * @param verification_process 
     */
    @JsonCreator
    public Verification(
            @JsonProperty("trust_framework") TrustFramework trust_framework,
            @JsonProperty("time") String time,
            @JsonProperty("verification_process") String verification_process) {
        this.trust_framework = trust_framework;
        this.time = time;
        this.verification_process = verification_process;
    }

    public TrustFramework getTrust_framework() { return this.trust_framework;  }
    public String getTime() { return this.time; }
    public String getVerification_process() { return this.verification_process; }

}
