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
public class IdentifyClaims implements Claims {

    private final String title_prefix;
    private final String title_suffix;
    private final String given_name;
    private final String family_name;
    private final String middle_name;
    private final String phone_number;
    private final String email;
    private final ArrayList<Address> addresses;
    private final String birthdate;
    private final int age;
    private final String date_of_death;
    private final Gender gender;
    private final String birthnumber;
    private final String birthcountry;
    private final String birthplace;
    private final String primary_nationality;
    private final String[] nationalities;
    private final MaritalStatus maritalstatus;
    private final List<IDCard> idcards;
    private final List<String> paymentAccounts;
    private final ArrayList<PaymentAccountDetail> paymentAccountsDetails;
    private final boolean limited_legal_capacity;
    private final boolean majority;
    private final boolean pep;
    private final long updated_at;

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
     * @param birthplace
     * @param primary_nationality
     * @param nationalities
     * @param maritalstatus
     * @param idcards
     * @param paymentAccounts
     * @param paymentAccountsDetails
     * @param limited_legal_capacity
     * @param majority
     * @param pep
     * @param updated_at 
     */
    @JsonCreator
    public IdentifyClaims(
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
            @JsonProperty("maritalstatus") MaritalStatus maritalstatus,
            @JsonProperty("idcards") List<IDCard> idcards,
            @JsonProperty("paymentAccounts") List<String> paymentAccounts,
            @JsonProperty("paymentAccountsDetails") ArrayList<PaymentAccountDetail> paymentAccountsDetails,
            @JsonProperty("limited_legal_capacity") boolean limited_legal_capacity,
            @JsonProperty("majority") boolean majority,
            @JsonProperty("pep") boolean pep,
            @JsonProperty("updated_at") long updated_at) {
        this.title_prefix = title_prefix;
        this.title_suffix = title_suffix;
        this.given_name = given_name;
        this.family_name = family_name;
        this.middle_name = middle_name;
        this.phone_number = phone_number;
        this.email = email;
        this.addresses = addresses;
        this.birthdate = birthdate;
        this.age = age;
        this.date_of_death = date_of_death;
        this.gender = gender;
        this.birthnumber = birthnumber;
        this.birthcountry = birthcountry;
        this.birthplace = birthplace;
        this.primary_nationality = primary_nationality;
        this.nationalities = nationalities;
        this.maritalstatus = maritalstatus;
        this.idcards = idcards;
        this.paymentAccounts = paymentAccounts;
        this.paymentAccountsDetails = paymentAccountsDetails;
        this.limited_legal_capacity = limited_legal_capacity;
        this.majority = majority;
        this.pep = pep;
        this.updated_at = updated_at;
    }

    public String getTitle_prefix() { return this.title_prefix; }
    public String getTitle_suffix() { return this.title_suffix; }
    public String getGiven_name() { return this.given_name; }
    public String getFamily_name() { return this.family_name; }
    public String getMiddle_name() { return this.middle_name; }
    public String getPhone_number() { return this.phone_number; }
    public String getEmail() { return this.email; }
    public List<Address> getAddresses() { return this.addresses; }
    public String getBirthdate() { return this.birthdate; }
    public int getAge() { return this.age; }
    public String getDate_of_death() { return this.date_of_death; }
    public Gender getGender() { return this.gender; }
    public String getBirthnumber() { return this.birthnumber; }
    public String getBirthcountry() { return this.birthcountry; }
    public String getBirthplace() { return this.birthplace; }
    public String getPrimary_nationality() { return this.primary_nationality; }
    public String[] getNationalities() { return this.nationalities; }
    public MaritalStatus getMaritalstatus() { return this.maritalstatus; }
    public List<IDCard> getIdcards() { return this.idcards; }
    public List<String> getPaymentAccounts() { return this.paymentAccounts; }
    public ArrayList<PaymentAccountDetail> getPaymentAccountsDetails() { return this.paymentAccountsDetails; }
    public boolean getLimited_legal_capacity() { return this.limited_legal_capacity; }
    public boolean getMajority() { return this.majority; }
    public boolean getPep() { return this.pep; }
    public long getUpdated_at() { return this.updated_at; }

}
