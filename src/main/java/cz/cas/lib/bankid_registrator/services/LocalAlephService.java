package cz.cas.lib.bankid_registrator.services;

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
import cz.cas.lib.bankid_registrator.util.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@Profile("local")
public class LocalAlephService extends AlephService implements AlephServiceIface
{
    public LocalAlephService(MainConfiguration mainConfig, AlephServiceConfig alephServiceConfig, IdentityService identityService, OracleRepository oracleRepository, ResourceLoader resourceLoader)
    {
        super(mainConfig, alephServiceConfig, identityService, oracleRepository, resourceLoader);

        this.borXOpsNoSuccessMsg = new String[] {
            PatronBorXOp.BOR_INFO.getValue(),
            PatronBorXOp.BOR_AUTH.getValue()
        };
    }

    /**
     * Initializes patron's testing data based on the Connect and Identify objects
     * @param userInfo
     * @param userProfile
     * @return  Map<String, Object>
     */
    @Override
    public Map<String, Object> newPatron(Connect userInfo, Identify userProfile)
    {
        getLogger().info("Executing newPatronLocal");
        Assert.notNull(userInfo, "\"userInfo\" is required");
        Assert.notNull(userProfile, "\"userProfile\" is required");

        Map<String, Object> result = new HashMap<>();

        Patron patron = new Patron();

        // String fname = userInfo.getGiven_name();      // Joanne
        // String mname = this.generateTestingMname();   // Kathleen
        // String lname = userInfo.getFamily_name();     // Rowling

        String fname = StringUtils.capitalizeIfUppercase(userInfo.getGiven_name());      // Joanne
        String mname = StringUtils.capitalizeIfUppercase(this.generateTestingMname());     // Kathleen
        String lname = StringUtils.capitalizeIfUppercase(userInfo.getFamily_name());     // Rowling

        patron.setLastname(lname);
        patron.setFirstname(Stream.of(fname, mname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Joanne Kathleen
        patron.setName(Stream.of(lname, mname, fname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Rowling Kathleen Joanne

        patron.setEmail(Optional.ofNullable(this.generateTestingEmail()).orElse(""));

        String birthdate = userInfo.getBirthdate();   // 2003-07-25
        if (birthdate == null) {
            result.put("error", "chybí datum narození");
            return result;
        }
        patron.setBirthDate(birthdate.replace("-", ""));  // 2003-07-25 => 20030725

        String smsNumber = this.generateTestingPhone(); logger.info("smsNumber: {}", smsNumber);
        patron.setSmsNumber(Optional.ofNullable(smsNumber).orElse(""));

        String conLng = userInfo.getLocale(); logger.info("conLng: {}", conLng);
        patron.setConLng((conLng == null || conLng.equals("cs_CZ")) ? PatronLanguage.CZE : PatronLanguage.ENG); logger.info("patron.getConLng() == {}", patron.getConLng());

        // Patron address names
        patron.setAddress0(patron.getName());
        patron.setContactAddress0(patron.getName());

        // Check if the BankID address data is available
        if (userProfile.getAddresses() == null) {
            result.put("error", "v BankID účtu nemáte nastavenou adresu");
            return result;
        }

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

        patron.setVerification(this.generatePatronPassword());

        // BankID
        patron.setBankIdSub(userInfo.getSub());

        // // New registration or registration renewal
        // boolean isNewAlephPatron = this.isNewAlephPatron(patron);logger.info("isNewAlephPatron: {}", isNewAlephPatron);
        // patron.isNew = isNewAlephPatron;
        // patron.setAction(isNewAlephPatron ? PatronAction.I : PatronAction.A); logger.info("patron.getAction(): {}", patron.getAction());

        // result.put("patron", patron);

        // return result;

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

    public String generateTestingMname() {
        StringBuilder name = new StringBuilder("Test");
        Random random = new Random();
        int lengthOfRandomString = 5;

        for (int i = 0; i < lengthOfRandomString; i++) {
            char randomChar = (char) ('a' + random.nextInt(26));
            name.append(randomChar);
        }

        return name.toString();
    }

    // private String generateTestingBirthday() {
    //     Random random = new Random();
    //     int minDay = (int) LocalDate.of(1950, 1, 1).toEpochDay();
    //     int maxDay = (int) LocalDate.of(2010, 12, 31).toEpochDay();
    //     long randomDay = minDay + random.nextInt(maxDay - minDay);
    //     return LocalDate.ofEpochDay(randomDay).toString();
    // }

    private String generateTestingEmail() {
        Random random = new Random();
        int randomNum = 100000 + random.nextInt(900000);
        return "test" + randomNum + "@testing.com";
    }

    private String generateTestingPhone() {
        Random random = new Random();
        int randomNum = 100000000 + random.nextInt(900000000);
        return "+420" + randomNum;
    }

    // private String generateTestingBankIdSub() {
    //     Random random = new Random();
    //     int randomNum = 10000 + random.nextInt(90000);
    //     return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + randomNum;
    // }
}
