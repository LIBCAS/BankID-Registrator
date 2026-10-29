package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.model.voucher.VoucherAccountingCounter;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface VoucherAccountingCounterRepository extends JpaRepository<VoucherAccountingCounter, Long> {
    @Modifying
    @Query(value = "INSERT INTO voucher_accounting_counter (id, last_number) VALUES (1, 0) " +
            "ON DUPLICATE KEY UPDATE id = id", nativeQuery = true)
    void ensureExists();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM VoucherAccountingCounter c WHERE c.id = 1")
    VoucherAccountingCounter lockCounter();
}
