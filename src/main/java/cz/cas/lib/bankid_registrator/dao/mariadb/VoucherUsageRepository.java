package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import cz.cas.lib.bankid_registrator.model.voucher.VoucherUsage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long>
{
    boolean existsByVoucherAndIdentity(Voucher voucher, Identity identity);

    boolean existsByVoucherAndIdentityAndConfirmedTrue(Voucher voucher, Identity identity);

    List<VoucherUsage> findByVoucher(Voucher voucher);

    List<VoucherUsage> findByIdentity(Identity identity);

    List<VoucherUsage> findByIdentityOrderByUsedAtDesc(Identity identity);

    List<VoucherUsage> findByIdentityAndConfirmedFalseOrderByUsedAtDesc(Identity identity);

    Optional<VoucherUsage> findByVoucherAndIdentityAndConfirmedFalse(Voucher voucher, Identity identity);

    List<VoucherUsage> findByVoucherAndConfirmedTrue(Voucher voucher);

    List<VoucherUsage> findByConfirmedFalse();
}
