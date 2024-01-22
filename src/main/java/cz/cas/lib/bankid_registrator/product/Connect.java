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
package cz.cas.lib.bankid_registrator.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cas.lib.bankid_registrator.entities.ConnectClaims;
import cz.cas.lib.bankid_registrator.entities.ConnectVerifiedClaims;

/**
 *
 * @author iok
 */
public class Connect extends ConnectClaims implements IProduct {

    private final String sub;
    private final String txn;
    private final ConnectVerifiedClaims verified_claims;

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
     * @param sub
     * @param txn 
     * @param verified_claims 
     */
    @JsonCreator
    public Connect(
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
            @JsonProperty("updated_at") long updated_at,
            @JsonProperty("sub") String sub,
            @JsonProperty("txn") String txn,
            @JsonProperty("verified_claims") ConnectVerifiedClaims verified_claims) {
        super(
                name,
                given_name,
                family_name,
                middle_name,
                nickname,
                preferred_username,
                email,
                email_verified,
                gender,
                birthdate,
                zoneinfo,
                locale,
                phone_number,
                phone_number_verified,
                updated_at);
        this.sub = sub;
        this.txn = txn;
        this.verified_claims = verified_claims;
    }

    @Override
    public String getSub() { return this.sub; }

    @Override
    public String getTxn() { return this.txn; }

    public ConnectVerifiedClaims getVerified_claims() { return this.verified_claims; }

}
