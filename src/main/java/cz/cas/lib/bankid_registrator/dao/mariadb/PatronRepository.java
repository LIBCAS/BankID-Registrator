package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.patron.Patron;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PatronRepository extends JpaRepository<Patron, Long> {
    /**
     * Counts the number of Patron entities where the bankIdSub attribute is equal to the provided value.
     * @param bankIdSub the value to match the bankIdSub attribute against
     * @return the number of matching Patron entities
     */
    long countByBankIdSub(String bankIdSub);

    /**
     * Finds patrons (with their media files) who are CAS employees.
     * @return
     */
    @Query("SELECT distinct p FROM Patron p LEFT JOIN FETCH p.media WHERE p.isCasEmployee = true")
    List<Patron> findCasEmployeesWithMedia();
}