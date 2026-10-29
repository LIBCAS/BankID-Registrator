package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.PersistenceMariadbConfiguration;
import cz.cas.lib.bankid_registrator.dao.mariadb.VoucherAccountingCounterRepository;
import cz.cas.lib.bankid_registrator.dao.mariadb.VoucherRepository;
import cz.cas.lib.bankid_registrator.entities.voucher.DiscountType;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherStatus;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;

/** Uses only an explicitly supplied, disposable MariaDB schema. */
@EnabledIfEnvironmentVariable(named = "ACCOUNTING_TEST_JDBC_URL", matches = ".*/voucher_accounting_test")
@SpringJUnitConfig({PersistenceMariadbConfiguration.class, VoucherAccountingService.class, VoucherService.class})
@TestPropertySource(properties = {
        "spring.datasource.jdbc-url=${ACCOUNTING_TEST_JDBC_URL}",
        "spring.datasource.username=root",
        "spring.datasource.password=accounting-test-only",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.minimumIdle=1",
        "spring.datasource.hikari.maximumPoolSize=8"
})
class VoucherAccountingServiceTest {
    @Autowired private VoucherAccountingService accounting;
    @Autowired private VoucherService service;
    @Autowired private VoucherRepository vouchers;
    @Autowired private VoucherAccountingCounterRepository counters;
    @Autowired private DataSource dataSource;
    @Autowired @Qualifier("mariadbTransactionManager") private PlatformTransactionManager transactionManager;

    @BeforeEach
    void reset() {
        vouchers.deleteAll();
        counters.deleteAll();
        accounting.initialize();
    }

    @Test
    void backfillIsOrderedStableAndPreservesHistoricalTimestamps() {
        Voucher first = vouchers.save(voucher("OLD-FIRST"));
        Voucher second = vouchers.save(voucher("OLD-SECOND"));
        new JdbcTemplate(dataSource).update("UPDATE voucher SET created_at = '2020-01-02 03:04:05', " +
                "updated_at = '2021-02-03 04:05:06' WHERE id = ?", first.getId());
        first = vouchers.findById(first.getId()).orElseThrow();
        LocalDateTime originalCreated = first.getCreatedAt();
        LocalDateTime originalUpdated = first.getUpdatedAt();
        accounting.initialize();
        accounting.initialize();

        first = vouchers.findById(first.getId()).orElseThrow();
        assertEquals(1L, first.getAccountingNumber());
        assertEquals(2L, vouchers.findById(second.getId()).orElseThrow().getAccountingNumber());
        assertEquals(originalCreated, first.getCreatedAt());
        assertEquals(originalUpdated, first.getUpdatedAt());
        assertEquals(3L, service.save(voucher("NEW-AFTER-BACKFILL")).getAccountingNumber());
    }

    @Test
    void singleAndBulkCreationCountByOneDespiteDatabaseIncrementOfThree() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertEquals(3, jdbc.queryForObject("SELECT @@session.auto_increment_increment", Integer.class));
        assertEquals("InnoDB", jdbc.queryForObject("SELECT ENGINE FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voucher_accounting_counter'", String.class));
        Voucher single = service.save(voucher("SINGLE"));
        List<Voucher> batch = service.bulkGenerate(3, DiscountType.PERCENTAGE, new BigDecimal("100"),
                1, null, null, null, null, null, null, null);

        assertEquals(1L, single.getAccountingNumber());
        assertEquals(List.of(2L, 3L, 4L), numbers(batch));
        assertEquals(single.getId() + 3, batch.get(0).getId());
        assertEquals(batch.get(0).getId() + 3, batch.get(1).getId());
        assertEquals(4L, counters.findById(1L).orElseThrow().getLastNumber());
    }

    @Test
    void failedBatchRollsBackEveryVoucherAndDoesNotConsumeAccountingNumbers() {
        Voucher existing = service.save(voucher("DUPLICATE"));
        assertThrows(DataIntegrityViolationException.class,
                () -> accounting.create(List.of(voucher("ROLLED-BACK"), voucher("DUPLICATE"))));
        assertFalse(vouchers.existsByCode("ROLLED-BACK"));
        assertEquals(1L, counters.findById(1L).orElseThrow().getLastNumber());
        assertEquals(2L, service.save(voucher("NEXT")).getAccountingNumber());
        assertEquals(1L, vouchers.findById(existing.getId()).orElseThrow().getAccountingNumber());
    }

    @Test
    void parallelCreatorsAllocateUniqueConsecutiveNumbers() throws Exception {
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Long>> requests = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                final String code = "CONCURRENT-" + i;
                requests.add(() -> service.save(voucher(code)).getAccountingNumber());
            }
            List<Long> allocated = new ArrayList<>();
            for (var result : executor.invokeAll(requests, 30, TimeUnit.SECONDS)) {
                allocated.add(result.get());
            }
            Collections.sort(allocated);
            assertEquals(java.util.stream.LongStream.rangeClosed(1, 16).boxed().collect(Collectors.toList()), allocated);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void retriesWholeTransactionAfterConflictIncludingPreviouslyAssignedInternalIds() {
        PlatformTransactionManager conflictOnce = mock(PlatformTransactionManager.class, delegatesTo(transactionManager));
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(call -> {
            if (attempts.incrementAndGet() == 1) {
                transactionManager.rollback(call.getArgument(0));
                throw new org.springframework.transaction.TransactionSystemException("Simulated Galera COMMIT conflict",
                        new SQLException("Deadlock", "40001", 1213));
            }
            transactionManager.commit(call.getArgument(0));
            return null;
        }).when(conflictOnce).commit(any());
        VoucherAccountingService retrying = new VoucherAccountingService(vouchers, counters, conflictOnce);

        List<Voucher> result = retrying.create(List.of(voucher("RETRY-FIRST"), voucher("RETRY-SECOND")));

        assertEquals(List.of(1L, 2L), numbers(result));
        assertEquals(2L, vouchers.count());
        assertEquals(2L, counters.findById(1L).orElseThrow().getLastNumber());
        verify(conflictOnce, times(2)).commit(any());
    }

    @Test
    void editsAndDeactivationPreserveNumberAndJsonDoesNotExposeIt() throws Exception {
        Voucher original = service.save(voucher("UNCHANGING"));
        original.setAccountingNumber(999L);
        original.setNote("Edited");
        service.save(original);
        service.deactivate(original.getId());
        accounting.initialize();

        Voucher reloaded = vouchers.findById(original.getId()).orElseThrow();
        assertEquals(1L, reloaded.getAccountingNumber());
        assertEquals(VoucherStatus.INACTIVE, reloaded.getStatus());
        assertEquals("Edited", reloaded.getNote());
        // Avoid serializing lazy usages; only property visibility matters here.
        Voucher jsonVoucher = voucher("JSON");
        jsonVoucher.setAccountingNumber(1L);
        assertFalse(new ObjectMapper().writeValueAsString(jsonVoucher).contains("accounting"));
    }

    private static Voucher voucher(String code) {
        return new Voucher(code, DiscountType.PERCENTAGE, new BigDecimal("100"), 1);
    }

    private static List<Long> numbers(List<Voucher> values) {
        return values.stream().map(Voucher::getAccountingNumber).collect(Collectors.toList());
    }
}
