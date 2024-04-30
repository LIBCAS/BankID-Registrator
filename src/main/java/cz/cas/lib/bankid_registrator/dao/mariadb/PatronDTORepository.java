package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.dto.PatronDTO;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PatronDTORepository extends JpaRepository<PatronDTO, Long> {
    /**
     * Counts the number of PatronDTO entities where the bankIdSub attribute is equal to the provided value.
     * @param bankIdSub the value to match the bankIdSub attribute against
     * @return the number of matching PatronDTO entities
     */
    long countByBankIdSub(String bankIdSub);

    /**
     * Finds PatronDTO entities (with their media files) who are CAS employees.
     * @return
     */
    @Query("SELECT distinct p FROM PatronDTO p LEFT JOIN FETCH p.media WHERE p.isCasEmployee = true")
    List<PatronDTO> findCasEmployeesWithMedia();
}