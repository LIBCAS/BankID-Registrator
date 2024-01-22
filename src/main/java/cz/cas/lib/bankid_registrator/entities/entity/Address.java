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
public class Address {

    private final AddressType type;
    private final String street;
    private final String buildingapartment;
    private final String streetnumber;
    private final String evidencenumber;
    private final String city;
    private final String cityarea;
    private final String zipcode;
    private final String country;
    private final String ruian_reference;

    /**
     * 
     * @param type
     * @param street
     * @param buildingapartment
     * @param streetnumber
     * @param evidencenumber
     * @param city
     * @param cityarea
     * @param zipcode
     * @param country
     * @param ruian_reference 
     */
    @JsonCreator
    public Address(
            @JsonProperty("type") AddressType type,
            @JsonProperty("street") String street,
            @JsonProperty("buildingapartment") String buildingapartment,
            @JsonProperty("streetnumber") String streetnumber,
            @JsonProperty("evidencenumber") String evidencenumber,
            @JsonProperty("city") String city,
            @JsonProperty("cityarea") String cityarea,
            @JsonProperty("zipcode") String zipcode,
            @JsonProperty("country") String country,
            @JsonProperty("ruian_reference") String ruian_reference) {
        this.type = type;
        this.street = street;
        this.buildingapartment = buildingapartment;
        this.streetnumber = streetnumber;
        this.evidencenumber = evidencenumber;
        this.city = city;
        this.cityarea = cityarea;
        this.zipcode = zipcode;
        this.country = country;
        this.ruian_reference = ruian_reference;
    }

    public AddressType getType() { return this.type; }
    public String getStreet() { return this.street; }
    public String getBuildingapartment() { return this.buildingapartment; }
    public String getStreetnumber() { return this.streetnumber; }
    public String getEvidencenumber() { return this.evidencenumber; }
    public String getCity() { return this.city; }
    public String getCityarea() { return this.cityarea; }
    public String getZipcode() { return this.zipcode; }
    public String getCountry() { return this.country; }
    public String getRuian_reference() { return this.ruian_reference; }

}
