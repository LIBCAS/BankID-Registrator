package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.PatronRepository;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.exceptions.PatronNotFoundException;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.util.StringUtils;

import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class PatronService extends PatronServiceAbstract
{
    private final PatronRepository patronRepository;
    private final ModelMapper modelMapper;

    public PatronService(PatronRepository patronRepository, ModelMapper modelMapper) {
        this.patronRepository = patronRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Checks if the application is currently working with the given bankIdSub.
     * @param bankIdSub
     * @return
     */
    public boolean isProcessing(String bankIdSub) {
        return patronRepository.countByBankIdSub(bankIdSub) > 0;
    }

    /**
     * Get patron's DTO from patron
     * @param patron
     */
    public PatronDTO getPatronDTO(Patron patron) {
        PatronDTO patronDTO = modelMapper.map(patron, PatronDTO.class);

        patronDTO.setUseContactAddress(!StringUtils.isEmpty(patron.getContactAddress1(), patron.getContactAddress2(), patron.getContactZip()));

        return patronDTO;
    }

    /**
     * Get patron's DTO by patron id
     * @param id
     */
    public PatronDTO getPatronById(Long id) {
        Patron patron = patronRepository.findById(id)
            .orElseThrow(() -> new PatronNotFoundException());
        return getPatronDTO(patron);
    }

   /**
     * Gets the bankIdSub attribute of the Patron entity with the provided id.
     * @param id
     * @return
     */
    public String getBankIdSubById(Long id) {
        Optional<String> patronBankIdSubOpt = patronRepository.findBankIdSubById(id);

        return patronBankIdSubOpt.orElse(null);
    }

    /**
     * Gets the patronId attribute of the Patron entity with the provided id.
     * @param id
     * @return
     */
    public String getPatronIdById(Long id) {
        Optional<String> patronIdOpt = patronRepository.findPatronIdById(id);

        // if (!patronRepository.existsById(id)) {
        //     throw new PatronNotFoundException("Patron with app ID " + id + " does not exist");
        // }

        return patronIdOpt.orElse(null);
    }

    /**
     * Merges two patrons into one. If the fields are the same, the value is taken from the first patron.
     * @param bankIdPatron - patron with data provided by the BankID
     * @param alephPatron - patron with data provided by the user during the initial registration
     * @return
     */
    public static Patron mergePatrons(Patron bankIdPatron, Patron alephPatron) {
        Patron latestPatron = new Patron();

        latestPatron.setNew(false);
        latestPatron.setPatronId(alephPatron.getPatronId());
        latestPatron.setFirstname(bankIdPatron.getFirstname());
        latestPatron.setLastname(bankIdPatron.getLastname());
        latestPatron.setName(bankIdPatron.getName());
        latestPatron.setEmail(mergeField(bankIdPatron.getEmail(), alephPatron.getEmail()));
        latestPatron.setBirthDate(bankIdPatron.getBirthDate());
        latestPatron.setConLng(alephPatron.getConLng());
        latestPatron.setAddress0(bankIdPatron.getAddress0());
        latestPatron.setAddress1(bankIdPatron.getAddress1());
        latestPatron.setAddress2(bankIdPatron.getAddress2());
        latestPatron.setZip(bankIdPatron.getZip());
        latestPatron.setContactAddress0(bankIdPatron.getAddress0());
        latestPatron.setContactAddress1(mergeField(bankIdPatron.getContactAddress1(), alephPatron.getContactAddress1()));
        latestPatron.setContactAddress2(mergeField(bankIdPatron.getContactAddress2(), alephPatron.getContactAddress2()));
        latestPatron.setContactZip(mergeField(bankIdPatron.getContactZip(), alephPatron.getContactZip()));
        latestPatron.setSmsNumber(mergeField(bankIdPatron.getSmsNumber(), alephPatron.getSmsNumber()));
        latestPatron.setStatus(alephPatron.getStatus());
        latestPatron.setBarcode(alephPatron.getBarcode());
        latestPatron.setIdCardName(mergeField(bankIdPatron.getIdCardName(), alephPatron.getIdCardName()));
        latestPatron.setIdCardNumber(mergeField(bankIdPatron.getIdCardNumber(), alephPatron.getIdCardNumber()));
        latestPatron.setIdCardDetail(mergeField(bankIdPatron.getIdCardDetail(), alephPatron.getIdCardDetail()));
        latestPatron.setVerification(alephPatron.getVerification());
        latestPatron.setBankIdSub(bankIdPatron.getBankIdSub());
        latestPatron.setExportConsent(PatronBoolean.N); // the user has to give consent again even if they gave it before
        latestPatron.setIsCasEmployee(false); // the user has to confirm their CAS employee status again
        latestPatron.setRfid(mergeField(bankIdPatron.getRfid(), alephPatron.getRfid()));
        latestPatron.setExpiryDate(alephPatron.getExpiryDate());

        return latestPatron;
    }

    /**
     * Merges two fields. If the fields are the same, the value is taken from the first field.
     * @param field1
     * @param field2
     * @return
     */
    private static String mergeField(String field1, String field2) {
        if (field1 == null || field1.isEmpty()) {
            return field2;
        } else if (field2 == null || field2.isEmpty()) {
            return field1;
        } else if (field1.equals(field2)) {
            return field1;
        } else {
            return null;
        }
    }
}
