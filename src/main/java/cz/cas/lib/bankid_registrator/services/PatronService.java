package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.PatronDTORepository;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PatronService {
    private final PatronDTORepository patronDTORepository;

    public PatronService(PatronDTORepository patronDTORepository) {
        this.patronDTORepository = patronDTORepository;
    }

    /**
     * Checks if the application is currently working with the given bankIdSub.
     * @param bankIdSub
     * @return
     */
    public boolean isProcessing(String bankIdSub) {
        return patronDTORepository.countByBankIdSub(bankIdSub) > 0;
    }

    /**
     * Finds PatronDTO entities (with their media files) who are CAS employees.
     * @return
     */
    public List<PatronDTO> findCasEmployeesWithMedia() {
        return patronDTORepository.findCasEmployeesWithMedia();
    }
}
