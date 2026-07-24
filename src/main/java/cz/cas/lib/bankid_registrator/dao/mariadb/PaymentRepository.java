package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.entities.payment.PaymentStatus;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.payment.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long>
{
    Optional<Payment> findById(Long id);

    Optional<Payment> findFirstByIdentity_AlephBarcodeOrderByCreatedAtDesc(String alephBarcode);

    List<Payment> findByIdentity(Identity identity);

    List<Payment> findByIdentityAndStatus(Identity identity, PaymentStatus status);

    Optional<Payment> findFirstByIdentityOrderByCreatedAtDesc(Identity identity);
}
