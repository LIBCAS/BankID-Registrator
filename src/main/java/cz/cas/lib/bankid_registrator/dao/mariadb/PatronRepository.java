package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.patron.Patron;
import java.util.Optional;
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
     * Gets the bankIdSub attribute of the Patron entity with the provided id.
     * @param id
     * @return
     */
    @Query("SELECT p.bankIdSub FROM Patron p WHERE p.id = :id")
    String findBankIdSubById(Long id);

    /**
     * Gets the patronId attribute of the Patron entity with the provided id.
     * @param id
     * @return
     */
    @Query("SELECT p.patronId FROM Patron p WHERE p.id = :id")
    Optional<String> findPatronIdById(Long id);
}