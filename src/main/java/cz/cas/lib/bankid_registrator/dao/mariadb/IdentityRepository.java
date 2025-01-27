package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.identity.Identity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

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

    @Query("SELECT i.alephId FROM Identity i WHERE i.alephId IS NOT NULL")
    List<String> findAllAlephIds();

    @Query(
        "SELECT DISTINCT i FROM Identity i " +
        "JOIN IdentityActivity ia ON i = ia.identity " +
        "WHERE (:filterSoftDeleted = true AND i.deleted = false OR :filterSoftDeleted = false) AND " +
        "i.alephDeleted = false AND " +
        "(:searchAlephIdOrBarcode IS NULL OR :searchAlephIdOrBarcode = '' OR " +
        "COALESCE(i.alephId, '') LIKE %:searchAlephIdOrBarcode% OR " +
        "COALESCE(i.alephBarcode, '') LIKE %:searchAlephIdOrBarcode%) AND " +
        "(:filterCasEmployee IS NULL OR i.isCasEmployee = :filterCasEmployee) AND " +
        "(:filterCheckedByAdmin IS NULL OR i.checkedByAdmin = :filterCheckedByAdmin) AND " +
        "(ia.activityEvent = cz.cas.lib.bankid_registrator.entities.activity.ActivityEvent.MEMBERSHIP_RENEWAL_SUCCESS OR " +
        "ia.activityEvent = cz.cas.lib.bankid_registrator.entities.activity.ActivityEvent.NEW_REGISTRATION_SUCCESS)"
    )
    Page<Identity> findIdentities(Pageable pageable, String searchAlephIdOrBarcode, Boolean filterCasEmployee, Boolean filterCheckedByAdmin, Boolean filterSoftDeleted);
}
