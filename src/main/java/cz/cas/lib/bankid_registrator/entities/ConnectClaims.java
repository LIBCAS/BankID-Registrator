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
package cz.cas.lib.bankid_registrator.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author iok
 */
public class ConnectClaims implements Claims {

    private final String name;
    private final String given_name;
    private final String family_name;
    private final String middle_name;
    private final String nickname;
    private final String preferred_username;
    private final String email;
    private final boolean email_verified;
    private final String gender;
    private final String birthdate;
    private final String zoneinfo;
    private final String locale;
    private final String phone_number;
    private final boolean phone_number_verified;
    private final long updated_at;

    /**
     * 
     * @param name
     * @param given_name
     * @param family_name
     * @param middle_name
     * @param nickname
     * @param preferred_username
     * @param email
     * @param email_verified
     * @param gender
     * @param birthdate
     * @param zoneinfo
     * @param locale
     * @param phone_number
     * @param phone_number_verified
     * @param updated_at 
     */
    @JsonCreator
    public ConnectClaims(
            @JsonProperty("name") String name,
            @JsonProperty("given_name") String given_name,
            @JsonProperty("family_name") String family_name,
            @JsonProperty("middle_name") String middle_name,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("preferred_username") String preferred_username,
            @JsonProperty("email") String email,
            @JsonProperty("email_verified") boolean email_verified,
            @JsonProperty("gender") String gender,
            @JsonProperty("birthdate") String birthdate,
            @JsonProperty("zoneifo") String zoneinfo,
            @JsonProperty("locale") String locale,
            @JsonProperty("phone_number") String phone_number,
            @JsonProperty("phone_number_verified") boolean phone_number_verified,
            @JsonProperty("updated_at") long updated_at) {
        this.name = name;
        this.given_name = given_name;
        this.family_name = family_name;
        this.middle_name = middle_name;
        this.nickname = nickname;
        this.preferred_username = preferred_username;
        this.email = email;
        this.email_verified = email_verified;
        this.gender = gender;
        this.birthdate = birthdate;
        this.zoneinfo = zoneinfo;
        this.locale = locale;
        this.phone_number = phone_number;
        this.phone_number_verified = phone_number_verified;
        this.updated_at = updated_at;
    }

    public String getName() { return this.name; }
    public String getGiven_name() { return this.given_name; }
    public String getFamily_name() { return this.family_name; }
    public String getMiddle_name() { return this.middle_name; }
    public String getNickname() { return this.nickname; }
    public String getPreferred_username() { return this.preferred_username; }
    public String getEmail() { return this.email; }
    public boolean getEmail_verified() { return this.email_verified; }
    public String getGender() { return this.gender; }
    public String getBirthdate() { return this.birthdate; }
    public String getZoneinfo() { return this.zoneinfo; }
    public String getLocale() { return this.locale; }
    public String getPhone_number() { return this.phone_number; }
    public boolean getPhone_number_verified() { return this.phone_number_verified; }
    public long getUpdated_at() { return this.updated_at; }

}
