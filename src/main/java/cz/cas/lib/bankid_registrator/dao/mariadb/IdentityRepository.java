package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.identity.Identity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface IdentityRepository extends JpaRepository<Identity, Long>
{
    Optional<Identity> findById(Long id);

    Optional<Identity> findByBankId(String bankId);

    Optional<Identity> findByAlephId(String alephId);

    Optional<Identity> findByAlephBarcode(String alephBarcode);

    @Query("SELECT i FROM Identity i WHERE SIZE(i.media) > 0")
    List<Identity> getIdentitiesWithMedia();

    @Query(value = "SELECT MAX(i.id) FROM identity i", nativeQuery = true)
    Long getMaxId();

    @Query("SELECT i FROM Identity i WHERE i.bankId = :bankId AND i.alephId IS NOT NULL AND i.alephBarcode IS NOT NULL")
    Optional<Identity> findAlephLinkedByBankId(String bankId);
}