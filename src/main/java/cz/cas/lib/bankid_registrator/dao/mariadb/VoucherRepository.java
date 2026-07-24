package cz.cas.lib.bankid_registrator.dao.mariadb;

import cz.cas.lib.bankid_registrator.entities.voucher.DiscountType;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherStatus;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherType;
import cz.cas.lib.bankid_registrator.dto.VoucherPrintData;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface VoucherRepository extends JpaRepository<Voucher, Long>
{
    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    List<Voucher> findByStatus(VoucherStatus status);

    @Query("SELECT DISTINCT v FROM Voucher v LEFT JOIN FETCH v.usages WHERE v.id IN :ids")
    List<Voucher> findAllWithUsagesByIdIn(Collection<Long> ids);

    @Query(value = "SELECT v FROM Voucher v WHERE " +
           "(:voucherSearch IS NULL OR :voucherSearch = '' OR v.code LIKE %:voucherSearch% OR v.note LIKE %:voucherSearch% OR v.buyer LIKE %:voucherSearch% OR v.paymentMethod LIKE %:voucherSearch% OR v.invoice LIKE %:voucherSearch%) AND " +
           "(v.status IN :statuses) AND " +
           "(:type IS NULL OR v.type = :type) AND " +
           "(:discountType IS NULL OR v.discountType = :discountType) AND " +
           "(:expiresFrom IS NULL OR v.expiresAt >= :expiresFrom) AND " +
           "(:expiresTo IS NULL OR v.expiresAt <= :expiresTo) AND " +
           "(:firstUsedFrom IS NULL OR v.firstUsedAt >= :firstUsedFrom) AND " +
           "(:firstUsedTo IS NULL OR v.firstUsedAt <= :firstUsedTo)",
           countQuery = "SELECT COUNT(v.id) FROM Voucher v WHERE " +
           "(:voucherSearch IS NULL OR :voucherSearch = '' OR v.code LIKE %:voucherSearch% OR v.note LIKE %:voucherSearch% OR v.buyer LIKE %:voucherSearch% OR v.paymentMethod LIKE %:voucherSearch% OR v.invoice LIKE %:voucherSearch%) AND " +
           "(v.status IN :statuses) AND " +
           "(:type IS NULL OR v.type = :type) AND " +
           "(:discountType IS NULL OR v.discountType = :discountType) AND " +
           "(:expiresFrom IS NULL OR v.expiresAt >= :expiresFrom) AND " +
           "(:expiresTo IS NULL OR v.expiresAt <= :expiresTo) AND " +
           "(:firstUsedFrom IS NULL OR v.firstUsedAt >= :firstUsedFrom) AND " +
           "(:firstUsedTo IS NULL OR v.firstUsedAt <= :firstUsedTo)")
    Page<Voucher> findVouchers(Pageable pageable, String voucherSearch, Collection<VoucherStatus> statuses, VoucherType type, DiscountType discountType,
                               LocalDateTime expiresFrom, LocalDateTime expiresTo, LocalDateTime firstUsedFrom, LocalDateTime firstUsedTo);

    @Query(value = "SELECT DISTINCT v FROM Voucher v JOIN v.usages u WHERE " +
           "(:voucherSearch IS NULL OR :voucherSearch = '' OR v.code LIKE %:voucherSearch% OR v.note LIKE %:voucherSearch% OR v.buyer LIKE %:voucherSearch% OR v.paymentMethod LIKE %:voucherSearch% OR v.invoice LIKE %:voucherSearch%) AND " +
           "u.patronAlephId LIKE %:patronId% AND " +
           "(v.status IN :statuses) AND " +
           "(:type IS NULL OR v.type = :type) AND " +
           "(:discountType IS NULL OR v.discountType = :discountType) AND " +
           "(:expiresFrom IS NULL OR v.expiresAt >= :expiresFrom) AND " +
           "(:expiresTo IS NULL OR v.expiresAt <= :expiresTo) AND " +
           "(:firstUsedFrom IS NULL OR v.firstUsedAt >= :firstUsedFrom) AND " +
           "(:firstUsedTo IS NULL OR v.firstUsedAt <= :firstUsedTo)",
           countQuery = "SELECT COUNT(DISTINCT v.id) FROM Voucher v JOIN v.usages u WHERE " +
           "(:voucherSearch IS NULL OR :voucherSearch = '' OR v.code LIKE %:voucherSearch% OR v.note LIKE %:voucherSearch% OR v.buyer LIKE %:voucherSearch% OR v.paymentMethod LIKE %:voucherSearch% OR v.invoice LIKE %:voucherSearch%) AND " +
           "u.patronAlephId LIKE %:patronId% AND " +
           "(v.status IN :statuses) AND " +
           "(:type IS NULL OR v.type = :type) AND " +
           "(:discountType IS NULL OR v.discountType = :discountType) AND " +
           "(:expiresFrom IS NULL OR v.expiresAt >= :expiresFrom) AND " +
           "(:expiresTo IS NULL OR v.expiresAt <= :expiresTo) AND " +
           "(:firstUsedFrom IS NULL OR v.firstUsedAt >= :firstUsedFrom) AND " +
           "(:firstUsedTo IS NULL OR v.firstUsedAt <= :firstUsedTo)")
    Page<Voucher> findVouchersByPatronId(Pageable pageable, String voucherSearch, String patronId,
                                         Collection<VoucherStatus> statuses, VoucherType type, DiscountType discountType,
                                         LocalDateTime expiresFrom, LocalDateTime expiresTo,
                                         LocalDateTime firstUsedFrom, LocalDateTime firstUsedTo);

    @Query("SELECT new cz.cas.lib.bankid_registrator.dto.VoucherPrintData(" +
           "v.id, v.code, v.createdAt, v.expiresAt, v.firstUsedAt, v.discountType, v.discountValue, v.recipientType) " +
           "FROM Voucher v WHERE " +
           "(:voucherSearch IS NULL OR :voucherSearch = '' OR v.code LIKE %:voucherSearch% OR v.note LIKE %:voucherSearch% OR v.buyer LIKE %:voucherSearch% OR v.paymentMethod LIKE %:voucherSearch% OR v.invoice LIKE %:voucherSearch%) AND " +
           "(v.status IN :statuses) AND " +
           "(:type IS NULL OR v.type = :type) AND " +
           "(:discountType IS NULL OR v.discountType = :discountType) AND " +
           "(:expiresFrom IS NULL OR v.expiresAt >= :expiresFrom) AND " +
           "(:expiresTo IS NULL OR v.expiresAt <= :expiresTo) AND " +
           "(:firstUsedFrom IS NULL OR v.firstUsedAt >= :firstUsedFrom) AND " +
           "(:firstUsedTo IS NULL OR v.firstUsedAt <= :firstUsedTo)")
    List<VoucherPrintData> findVoucherPrintData(Pageable pageable, String voucherSearch,
                                                Collection<VoucherStatus> statuses, VoucherType type, DiscountType discountType,
                                                LocalDateTime expiresFrom, LocalDateTime expiresTo,
                                                LocalDateTime firstUsedFrom, LocalDateTime firstUsedTo);

    @Query("SELECT DISTINCT new cz.cas.lib.bankid_registrator.dto.VoucherPrintData(" +
           "v.id, v.code, v.createdAt, v.expiresAt, v.firstUsedAt, v.discountType, v.discountValue, v.recipientType) " +
           "FROM Voucher v JOIN v.usages u WHERE " +
           "(:voucherSearch IS NULL OR :voucherSearch = '' OR v.code LIKE %:voucherSearch% OR v.note LIKE %:voucherSearch% OR v.buyer LIKE %:voucherSearch% OR v.paymentMethod LIKE %:voucherSearch% OR v.invoice LIKE %:voucherSearch%) AND " +
           "u.patronAlephId LIKE %:patronId% AND " +
           "(v.status IN :statuses) AND " +
           "(:type IS NULL OR v.type = :type) AND " +
           "(:discountType IS NULL OR v.discountType = :discountType) AND " +
           "(:expiresFrom IS NULL OR v.expiresAt >= :expiresFrom) AND " +
           "(:expiresTo IS NULL OR v.expiresAt <= :expiresTo) AND " +
           "(:firstUsedFrom IS NULL OR v.firstUsedAt >= :firstUsedFrom) AND " +
           "(:firstUsedTo IS NULL OR v.firstUsedAt <= :firstUsedTo)")
    List<VoucherPrintData> findVoucherPrintDataByPatronId(Pageable pageable, String voucherSearch, String patronId,
                                                          Collection<VoucherStatus> statuses, VoucherType type, DiscountType discountType,
                                                          LocalDateTime expiresFrom, LocalDateTime expiresTo,
                                                          LocalDateTime firstUsedFrom, LocalDateTime firstUsedTo);

    @Query("SELECT v FROM Voucher v WHERE v.status = 'ACTIVE' AND " +
           "(v.expiresAt IS NULL OR v.expiresAt > :now) AND " +
           "(v.maxUses = 0 OR v.currentUses < v.maxUses)")
    List<Voucher> findAllValid(LocalDateTime now);

    @Modifying
    @Query("UPDATE Voucher v SET v.status = 'EXPIRED' WHERE v.status = 'ACTIVE' AND v.expiresAt IS NOT NULL AND v.expiresAt < :now")
    int expireActiveVouchers(LocalDateTime now);
}
