package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.PatronRepository;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.exceptions.PatronNotFoundException;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class PatronService {
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
        return modelMapper.map(patron, PatronDTO.class);
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
}
