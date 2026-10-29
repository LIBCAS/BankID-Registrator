package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.VoucherAccountingCounterRepository;
import cz.cas.lib.bankid_registrator.dao.mariadb.VoucherRepository;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import cz.cas.lib.bankid_registrator.model.voucher.VoucherAccountingCounter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Allocates accounting numbers in the same transaction as voucher creation. */
@Service
public class VoucherAccountingService {
    private static final int MAX_ATTEMPTS = 5;
    private final VoucherRepository vouchers;
    private final VoucherAccountingCounterRepository counters;
    private final TransactionTemplate transaction;

    public VoucherAccountingService(VoucherRepository vouchers,
            VoucherAccountingCounterRepository counters,
            @Qualifier("mariadbTransactionManager") PlatformTransactionManager transactionManager) {
        this.vouchers = vouchers;
        this.counters = counters;
        this.transaction = new TransactionTemplate(transactionManager);
        // Each retry needs a fresh persistence context and must include COMMIT
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @PostConstruct
    public void initialize() {
        withRetry(() -> {
            backfillAndLock();
            return null;
        });
    }

    public List<Voucher> create(List<Voucher> newVouchers) {
        if (newVouchers.isEmpty()) {
            return newVouchers;
        }
        if (newVouchers.stream().anyMatch(v -> v.getId() != null)) {
            throw new IllegalArgumentException("Accounting allocation requires new vouchers");
        }
        if (newVouchers.stream().anyMatch(v -> v.getUsages() != null && !v.getUsages().isEmpty())) {
            throw new IllegalArgumentException("New vouchers cannot have existing usages");
        }
        return withRetry(() -> {
            VoucherAccountingCounter counter = backfillAndLock();
            List<Voucher> attemptVouchers = new ArrayList<>();
            for (Voucher template : newVouchers) {
                // A rolled-back entity retains Hibernate IDs and collection wrappers
                // Use fresh entities on every attempt, including their usages collections
                Voucher voucher = new Voucher();
                BeanUtils.copyProperties(template, voucher, "id", "accountingNumber", "usages", "createdAt", "updatedAt");
                counter.setLastNumber(Math.addExact(counter.getLastNumber(), 1));
                voucher.setAccountingNumber(counter.getLastNumber());
                attemptVouchers.add(voucher);
            }
            counters.saveAndFlush(counter);
            return vouchers.saveAllAndFlush(attemptVouchers);
        });
    }

    private VoucherAccountingCounter backfillAndLock() {
        counters.ensureExists();
        VoucherAccountingCounter counter = counters.lockCounter();
        counter.setLastNumber(Math.max(counter.getLastNumber(), vouchers.findMaximumAccountingNumber()));
        for (Long id : vouchers.findIdsWithoutAccountingNumber()) {
            counter.setLastNumber(Math.addExact(counter.getLastNumber(), 1));
            if (vouchers.assignAccountingNumber(id, counter.getLastNumber()) != 1) {
                throw new IllegalStateException("Voucher changed during accounting backfill: " + id);
            }
        }
        counters.saveAndFlush(counter);
        return counter;
    }

    private <T> T withRetry(Supplier<T> work) {
        for (int attempt = 1; ; attempt++) {
            try {
                return transaction.execute(status -> work.get());
            } catch (RuntimeException failure) {
                if (attempt >= MAX_ATTEMPTS || !isConcurrencyConflict(failure)) {
                    throw failure;
                }
                try {
                    Thread.sleep(attempt * 25L + ThreadLocalRandom.current().nextLong(25));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while retrying voucher creation", interrupted);
                }
            }
        }
    }

    private static boolean isConcurrencyConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConcurrencyFailureException) {
                return true;
            }
            if (cause instanceof SQLException) {
                SQLException sql = (SQLException) cause;
                if ("40001".equals(sql.getSQLState()) || sql.getErrorCode() == 1213 || sql.getErrorCode() == 1205) {
                    return true;
                }
            }
        }
        // Do not retry connection failures: a lost COMMIT response has an unknown outcome
        return false;
    }
}
