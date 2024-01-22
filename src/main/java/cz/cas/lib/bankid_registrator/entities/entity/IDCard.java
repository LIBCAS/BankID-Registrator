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
public class IDCard {

    private final IDCardType type;
    private final String description;
    private final String country;
    private final String number;
    private final String valid_to;
    private final String issuer;
    private final String issue_date;

    /**
     * 
     * @param type
     * @param description
     * @param country
     * @param number
     * @param valid_to
     * @param issuer
     * @param issue_date 
     */
    @JsonCreator
    public IDCard(
            @JsonProperty("type") IDCardType type,
            @JsonProperty("description") String description,
            @JsonProperty("country") String country,
            @JsonProperty("number") String number,
            @JsonProperty("valid_to") String valid_to,
            @JsonProperty("issuer") String issuer,
            @JsonProperty("issue_date") String issue_date) {
        this.type = type;
        this.description = description;
        this.country = country;
        this.number = number;
        this.valid_to = valid_to;
        this.issuer = issuer;
        this.issue_date = issue_date;
    }

    public IDCardType getType() { return this.type; }
    public String getDescription() { return this.description; }
    public String getCountry() { return this.country; }
    public String getNumber() { return this.number; }
    public String getValid_to() { return this.valid_to; }
    public String getIssuer() { return this.issuer; }
    public String getIssue_date() { return this.issue_date; }

}
