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
import cz.cas.lib.bankid_registrator.entities.IdentifyPlusClaims;
import cz.cas.lib.bankid_registrator.entities.IdentifyPlusVerifiedClaims;
import cz.cas.lib.bankid_registrator.entities.entity.Address;
import cz.cas.lib.bankid_registrator.entities.entity.Gender;
import cz.cas.lib.bankid_registrator.entities.entity.IDCard;
import cz.cas.lib.bankid_registrator.entities.entity.MaritalStatus;
import cz.cas.lib.bankid_registrator.entities.entity.PaymentAccountDetail;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author iok
 */
public class IdentifyPlus extends IdentifyPlusClaims implements IProduct {

    private final String sub;
    private final String txn;
    private final IdentifyPlusVerifiedClaims verified_claims;

    /**
     * 
     * @param title_prefix
     * @param title_suffix
     * @param given_name
     * @param family_name
     * @param middle_name
     * @param phone_number
     * @param email
     * @param addresses
     * @param birthdate
     * @param age
     * @param date_of_death
     * @param gender
     * @param birthnumber
     * @param birthcountry
     * @param updated_at
     * @param birthplace
     * @param primary_nationality
     * @param nationalities
     * @param maritalstatus
     * @param idcards
     * @param majority
     * @param paymentAccountsDetails
     * @param pep
     * @param limited_legal_capacity
     * @param paymentAccounts
     * @param sub
     * @param txn
     * @param verified_claims 
     */
    @JsonCreator
    public IdentifyPlus(
            @JsonProperty("title_prefix") String title_prefix,
            @JsonProperty("title_suffix") String title_suffix,
            @JsonProperty("given_name") String given_name,
            @JsonProperty("family_name") String family_name,
            @JsonProperty("middle_name") String middle_name,
            @JsonProperty("phone_number") String phone_number,
            @JsonProperty("email") String email,
            @JsonProperty("addresses") ArrayList<Address> addresses,
            @JsonProperty("birthdate") String birthdate,
            @JsonProperty("age") int age,
            @JsonProperty("date_of_death") String date_of_death,
            @JsonProperty("gender") Gender gender,
            @JsonProperty("birthnumber") String birthnumber,
            @JsonProperty("birthcountry") String birthcountry,
            @JsonProperty("birthplace") String birthplace,
            @JsonProperty("primary_nationality") String primary_nationality,
            @JsonProperty("nationalities") String[] nationalities,
            @JsonProperty("updated_at") long updated_at,
            @JsonProperty("maritalstatus") MaritalStatus maritalstatus,
            @JsonProperty("idcards") List<IDCard> idcards,
            @JsonProperty("paymentAccounts") List<String> paymentAccounts,
            @JsonProperty("paymentAccountsDetails") ArrayList<PaymentAccountDetail> paymentAccountsDetails,
            @JsonProperty("majority") boolean majority,
            @JsonProperty("pep") boolean pep,
            @JsonProperty("limited_legal_capacity") boolean limited_legal_capacity,
            @JsonProperty("sub") String sub,
            @JsonProperty("txn") String txn,
            @JsonProperty("verified_claims") IdentifyPlusVerifiedClaims verified_claims) {
        super(
                title_prefix,
                title_suffix,
                given_name,
                family_name,
                middle_name,
                phone_number,
                email,
                addresses,
                birthdate,
                age,
                date_of_death,
                gender,
                birthnumber,
                birthcountry,
                birthplace,
                primary_nationality,
                nationalities,
                updated_at,
                maritalstatus,
                idcards,
                paymentAccounts,
                paymentAccountsDetails,
                majority,
                pep,
                limited_legal_capacity);
        this.sub = sub;
        this.txn = txn;
        this.verified_claims = verified_claims;
    }

    @Override
    public String getSub() { return this.sub; }

    @Override
    public String getTxn() { return this.txn; }

    public IdentifyPlusVerifiedClaims getVerified_claims() { return this.verified_claims; }

}
