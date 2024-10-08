package cz.cas.lib.bankid_registrator.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.cas.lib.bankid_registrator.configurations.AlephServiceConfig;
import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.dao.oracle.OracleRepository;
import cz.cas.lib.bankid_registrator.entities.entity.Address;
import cz.cas.lib.bankid_registrator.entities.entity.AddressType;
import cz.cas.lib.bankid_registrator.entities.entity.IDCard;
import cz.cas.lib.bankid_registrator.entities.entity.IDCardType;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBorXOp;
import cz.cas.lib.bankid_registrator.entities.patron.PatronLanguage;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;

@Service
@Profile("production")
public class ProductionAlephService extends AlephService implements AlephServiceIface
{
    public ProductionAlephService(MainConfiguration mainConfig, AlephServiceConfig alephServiceConfig, IdentityService identityService, OracleRepository oracleRepository)
    {
        super(mainConfig, alephServiceConfig, identityService, oracleRepository);

        this.borXOpsNoSuccessMsg = new String[] {
            PatronBorXOp.BOR_INFO.getValue(),
            PatronBorXOp.BOR_AUTH.getValue()
        };
    }

    /**
     * Initializes patron's data based on the Connect and Identify objects
     * @param userInfo
     * @param userProfile
     * @return  Map<String, Object>
     */
    @Override
    public Map<String, Object> newPatron(Connect userInfo, Identify userProfile)
    {
        getLogger().info("Executing newPatron");
        Assert.notNull(userInfo, "\"userInfo\" is required");
        Assert.notNull(userProfile, "\"userProfile\" is required");

        Map<String, Object> result = new HashMap<>();

        Patron patron = new Patron();

        String fname = userInfo.getGiven_name();      // Joanne
        String mname = Optional.ofNullable(userInfo.getMiddle_name()).orElse("");     // Kathleen
        String lname = userInfo.getFamily_name();     // Rowling

        patron.setFirstname(fname);
        patron.setLastname(Stream.of(lname, mname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Rowling Kathleen
        patron.setName(Stream.of(lname, mname, fname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Rowling Kathleen Joanne

        patron.setEmail(Optional.ofNullable(userInfo.getEmail()).orElse(""));

        String birthdate = userInfo.getBirthdate();   // 2003-07-25
        if (birthdate == null) {
            result.put("error", "chybí datum narození");
            return result;
        }
        patron.setBirthDate(birthdate.replace("-", ""));  // 2003-07-25 => 20030725

        String smsNumber = userProfile.getPhone_number();
        patron.setSmsNumber(Optional.ofNullable(smsNumber).orElse(""));

        String conLng = userInfo.getLocale(); logger.info("conLng: {}", conLng);
        patron.setConLng((conLng == null || conLng.equals("cs_CZ")) ? PatronLanguage.CZE : PatronLanguage.ENG); logger.info("patron.getConLng() == {}", patron.getConLng());

        // Patron address names
        patron.setAddress0(patron.getName());
        patron.setContactAddress0(patron.getName());

        // Patron address (permanent residence)
        Address address = userProfile.getAddresses().stream()
            .filter(a -> a.getType() == AddressType.PERMANENT_RESIDENCE)
            .findFirst()
            .orElse(null);
        if (address != null) {
            String addressStreet = Optional.ofNullable(address.getStreet()).orElse("");
            
            String addressNumber = "";
            if (address.getEvidencenumber() != null && !address.getEvidencenumber().isEmpty()) {
                addressNumber = address.getEvidencenumber();
            } else if (address.getBuildingapartment() != null && !address.getBuildingapartment().isEmpty() 
                    && address.getStreetnumber() != null && !address.getStreetnumber().isEmpty()) {
                addressNumber = address.getBuildingapartment() + "/" + address.getStreetnumber();
            } else if (address.getBuildingapartment() != null && !address.getBuildingapartment().isEmpty()) {
                addressNumber = address.getBuildingapartment();
            } else if (address.getStreetnumber() != null && !address.getStreetnumber().isEmpty()) {
                addressNumber = address.getStreetnumber();
            }

            String addressCity = address.getCity();
            if (addressCity == null) {
                result.put("error", "chybí město");
                return result;
            }
            String addressCityarea = Optional.ofNullable(address.getCityarea()).orElse("");
            String addressZip = address.getZipcode();
            if (addressZip == null) {
                result.put("error", "chybí PSČ");
                return result;
            }

            patron.setAddress1(Stream.of(!addressStreet.equals("") ? addressStreet : addressCityarea, addressNumber)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" ")));
            
            if (!addressCityarea.equals("") && addressCityarea.equals(addressCity)) {
                patron.setAddress2(addressCity);
            } else if (!addressCityarea.equals("") && !addressCityarea.equals(addressCity)) {
                patron.setAddress2(addressCity + " - " + addressCityarea);
            } else {
                patron.setAddress2(addressCity);
            }
            patron.setZip(addressZip);
        } else {
            result.put("error", "chybí adresa trvalého bydliště");
            return result;
        }

        // Patron contact address
        Address contactAddress = userProfile.getAddresses().stream()
            .filter(a -> a.getType() == AddressType.SECONDARY_RESIDENCE)
            .findFirst()
            .orElse(null);
        if (contactAddress != null) {
            String contactAddressStreet = Optional.ofNullable(contactAddress.getStreet()).orElse("");

            String contactAddressNumber = "";
            if (contactAddress.getEvidencenumber() != null && !contactAddress.getEvidencenumber().isEmpty()) {
                contactAddressNumber = contactAddress.getEvidencenumber();
            } else if (contactAddress.getBuildingapartment() != null && !contactAddress.getBuildingapartment().isEmpty() 
                    && contactAddress.getStreetnumber() != null && !contactAddress.getStreetnumber().isEmpty()) {
                        contactAddressNumber = contactAddress.getBuildingapartment() + "/" + contactAddress.getStreetnumber();
            } else if (contactAddress.getBuildingapartment() != null && !contactAddress.getBuildingapartment().isEmpty()) {
                contactAddressNumber = contactAddress.getBuildingapartment();
            } else if (contactAddress.getStreetnumber() != null && !contactAddress.getStreetnumber().isEmpty()) {
                contactAddressNumber = contactAddress.getStreetnumber();
            }

            String contactAddressCity = contactAddress.getCity();
            if (contactAddressCity == null) {
                result.put("error", "chybí město kontaktní adresy");
                return result;
            }
            String contactAddressCityarea = Optional.ofNullable(contactAddress.getCityarea()).orElse("");
            String contactAddressZip = contactAddress.getZipcode();
            if (contactAddressZip == null) {
                result.put("error", "chybí PSČ kontaktní adresy");
                return result;
            }

            patron.setContactAddress1(Stream.of(!contactAddressStreet.equals("") ? contactAddressStreet : contactAddressCityarea, contactAddressNumber)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" ")));
            
            if (!contactAddressCityarea.equals("") && contactAddressCityarea.equals(contactAddressCity)) {
                patron.setContactAddress2(contactAddressCity);
            } else if (!contactAddressCityarea.equals("") && !contactAddressCityarea.equals(contactAddressCity)) {
                patron.setContactAddress2(contactAddressCity + " - " + contactAddressCityarea);
            } else {
                patron.setContactAddress2(contactAddressCity);
            }
            patron.setContactZip(contactAddressZip);
        }

        // Patron ID card
        List<IDCard> idCards = userProfile.getIdcards();
        IDCard idCard = null;
        if (idCards != null) {
            idCard = idCards.stream()
                    .filter(i -> i.getType() == IDCardType.ID)
                    .findFirst()
                    .orElse(null);
        }
        if (idCard != null) {
            String idCardName = idCard.getType().toString().concat(" ").concat(idCard.getCountry());
            patron.setIdCardName(idCardName);
            patron.setIdCardNumber(idCard.getNumber());
            patron.setIdCardDetail("Občanský průkaz");
        } else {
            // result.put("error", "chybí občanský průkaz");
            // return result;
        }

        patron.setVerification(generatePatronPassword());

        // BankID
        patron.setBankIdSub(userInfo.getSub());

        // New registration or registration renewal
        Optional<String> existingAlephPatronIdOpt = this.getAlephPatronIdByNameAndBirth(patron);
        if (existingAlephPatronIdOpt.isPresent()) {
            String existingAlephPatronId = existingAlephPatronIdOpt.get();
            patron.setPatronId(existingAlephPatronId);
            patron.isNew = false;
        } else {
            patron.isNew = true;
        }

        result.put("patron", patron);

        return result;
    }
}