package cz.cas.lib.bankid_registrator.entities.patron;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PatronHold {
    public String pickUpLocation;
    public String lastInterestDate;
    public String note1;

    public String getPickUpLocation() {
        return pickUpLocation;
    }

    public void setPickUpLocation(String pickUpLocation) {
        this.pickUpLocation = pickUpLocation;
    }

    public String getLastInterestDate() {
        return lastInterestDate;
    }

    public void setLastInterestDate(String lastInterestDate) {
        this.lastInterestDate = lastInterestDate;
    }

    public String getNote1() {
        return note1;
    }

    public void setNote1(String note1) {
        this.note1 = note1;
    }

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }
}