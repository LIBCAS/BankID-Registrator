package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.dao.mariadb.VoucherRepository;
import cz.cas.lib.bankid_registrator.dao.mariadb.VoucherUsageRepository;
import cz.cas.lib.bankid_registrator.entities.voucher.DiscountType;
import cz.cas.lib.bankid_registrator.dto.VoucherPrintData;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherRecipientType;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherStatus;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import cz.cas.lib.bankid_registrator.model.voucher.VoucherUsage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for voucher (discount code) management and validation
 */
@Service
public class VoucherService extends ServiceAbstract
{
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no I/O/0/1
    private static final int DEFAULT_CODE_LENGTH = 8;

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    public VoucherService(VoucherRepository voucherRepository, VoucherUsageRepository voucherUsageRepository) {
        super(null);
        this.voucherRepository = voucherRepository;
        this.voucherUsageRepository = voucherUsageRepository;
    }

    // ========== Validation ==========

    /**
     * Validate a voucher code for a given identity and fee amount.
     * Returns a map with:
     *   "valid" -> boolean
     *   "error" -> String (error key for i18n, only when invalid)
     *   "voucher" -> Voucher (only when valid)
     *   "discountAmount" -> BigDecimal (computed discount in CZK, only when valid)
     *   "amountToPay" -> BigDecimal (fee after discount, only when valid)
     *
     * @param code The voucher code entered by the patron
     * @param identity The patron's identity
     * @param feeAmount The total fee amount in CZK (before discount)
     * @return validation result map
     */
    public Map<String, Object> validateVoucher(String code, Identity identity, BigDecimal feeAmount) {
        Map<String, Object> result = new HashMap<>();

        if (code == null || code.trim().isEmpty()) {
            result.put("valid", false);
            result.put("error", "voucher.error.empty");
            return result;
        }

        String normalizedCode = code.trim().toUpperCase();

        Optional<Voucher> voucherOpt = voucherRepository.findByCode(normalizedCode);
        if (!voucherOpt.isPresent()) {
            result.put("valid", false);
            result.put("error", "voucher.error.notFound");
            return result;
        }

        Voucher voucher = voucherOpt.get();

        // Check status — only ACTIVE vouchers can be applied
        if (voucher.getStatus() != VoucherStatus.ACTIVE) {
            // Auto-detect expired vouchers that still have ACTIVE status
            result.put("valid", false);
            if (voucher.getStatus() == VoucherStatus.EXPIRED) {
                result.put("error", "voucher.error.expired");
            } else if (voucher.getStatus() == VoucherStatus.APPLIED) {
                result.put("error", "voucher.error.maxUsesReached");
            } else {
                result.put("error", "voucher.error.inactive");
            }
            return result;
        }

        // Check expiration (and auto-update status if expired)
        if (voucher.getExpiresAt() != null && voucher.getExpiresAt().isBefore(LocalDateTime.now())) {
            voucher.setStatus(VoucherStatus.EXPIRED);
            voucherRepository.save(voucher);
            result.put("valid", false);
            result.put("error", "voucher.error.expired");
            return result;
        }

        // Check max uses (0 = unlimited) and auto-update status if fully used
        if (voucher.getMaxUses() > 0 && voucher.getCurrentUses() >= voucher.getMaxUses()) {
            voucher.setStatus(VoucherStatus.APPLIED);
            voucherRepository.save(voucher);
            result.put("valid", false);
            result.put("error", "voucher.error.maxUsesReached");
            return result;
        }

        // Check if this patron already used this voucher (confirmed usage)
        if (identity != null && voucherUsageRepository.existsByVoucherAndIdentityAndConfirmedTrue(voucher, identity)) {
            result.put("valid", false);
            result.put("error", "voucher.error.alreadyUsed");
            return result;
        }

        // Compute discount
        BigDecimal discountAmount = computeDiscount(voucher, feeAmount);
        BigDecimal amountToPay = feeAmount.subtract(discountAmount);
        if (amountToPay.compareTo(BigDecimal.ZERO) < 0) {
            amountToPay = BigDecimal.ZERO;
        }

        result.put("valid", true);
        result.put("voucher", voucher);
        result.put("discountAmount", discountAmount);
        result.put("amountToPay", amountToPay);
        return result;
    }

    /**
     * Compute the discount amount in CZK for a given voucher and fee amount
     */
    public BigDecimal computeDiscount(Voucher voucher, BigDecimal feeAmount) {
        return computeDiscount(voucher.getDiscountType(), voucher.getDiscountValue(), feeAmount);
    }

    public BigDecimal computeDiscount(DiscountType discountType, BigDecimal discountValue, BigDecimal feeAmount) {
        if (discountType == DiscountType.PERCENTAGE) {
            // percentage discount
            BigDecimal percentage = discountValue.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal discount = feeAmount.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
            // Cap at fee amount
            return discount.min(feeAmount);
        } else {
            // fixed amount discount
            return discountValue.min(feeAmount);
        }
    }

    // ========== Usage tracking ==========

    /**
     * Create a pending (unconfirmed) voucher usage record.
     * Called when the patron applies a voucher code before payment.
     * The usage is confirmed only when the payment succeeds.
     *
     * @param voucher The voucher
     * @param identity The patron's identity
     * @param discountApplied The actual discount amount applied in CZK
     * @return the created VoucherUsage
     */
    @Transactional
    public VoucherUsage createPendingUsage(Voucher voucher, Identity identity, BigDecimal discountApplied) {
        // Keep only one pending voucher usage per identity so changing the voucher
        // on the payment page replaces the previous pending voucher cleanly.
        List<VoucherUsage> existingPendingUsages = voucherUsageRepository.findByIdentityAndConfirmedFalseOrderByUsedAtDesc(identity);
        if (!existingPendingUsages.isEmpty()) {
            voucherUsageRepository.deleteAll(existingPendingUsages);
        }

        VoucherUsage usage = new VoucherUsage(voucher, identity, identity.getAlephId(), discountApplied);
        return voucherUsageRepository.save(usage);
    }

    /**
     * Confirm a pending voucher usage (called when payment succeeds).
     * Also increments the voucher's currentUses counter and updates
     * denormalized fields (patronIds, firstUsedAt).
     * If maxUses is reached, auto-sets the voucher status to APPLIED.
     *
     * @param voucher The voucher
     * @param identity The patron's identity
     */
    @Transactional
    public void confirmUsage(Voucher voucher, Identity identity) {
        Optional<VoucherUsage> usageOpt = voucherUsageRepository.findByVoucherAndIdentityAndConfirmedFalse(voucher, identity);
        if (usageOpt.isPresent()) {
            VoucherUsage usage = usageOpt.get();
            usage.setConfirmed(true);
            voucherUsageRepository.save(usage);

            voucher.setCurrentUses(voucher.getCurrentUses() + 1);
            updateDenormalizedFields(voucher);

            if (voucher.getMaxUses() > 0 && voucher.getCurrentUses() >= voucher.getMaxUses()) {
                voucher.setStatus(VoucherStatus.APPLIED);
            }
            voucherRepository.save(voucher);
        }
    }

    /**
     * Confirm a pending voucher usage, or create+confirm one if the pending record
     * is missing (e.g. due to a race condition where handlePaymentFailure cancelled
     * the pending usage before the payment was actually verified as successful).
     * Skips silently if the voucher was already confirmed for this identity.
     *
     * @param voucher The voucher
     * @param identity The patron's identity
     * @param discountApplied The discount amount (used only when creating a new record)
     */
    @Transactional
    public void confirmOrCreateUsage(Voucher voucher, Identity identity, BigDecimal discountApplied) {
        // Already confirmed → nothing to do
        if (voucherUsageRepository.existsByVoucherAndIdentityAndConfirmedTrue(voucher, identity)) {
            return;
        }

        // Try to confirm an existing pending usage
        Optional<VoucherUsage> usageOpt = voucherUsageRepository.findByVoucherAndIdentityAndConfirmedFalse(voucher, identity);
        if (usageOpt.isPresent()) {
            VoucherUsage usage = usageOpt.get();
            usage.setConfirmed(true);
            voucherUsageRepository.save(usage);
        } else {
            // Pending usage was cancelled or never created — create a confirmed record
            VoucherUsage usage = new VoucherUsage(voucher, identity, identity.getAlephId(), discountApplied);
            usage.setConfirmed(true);
            voucherUsageRepository.save(usage);
        }

        // Refresh voucher from DB to get current counter
        Voucher freshVoucher = voucherRepository.findById(voucher.getId()).orElse(voucher);
        freshVoucher.setCurrentUses(freshVoucher.getCurrentUses() + 1);
        updateDenormalizedFields(freshVoucher);

        if (freshVoucher.getMaxUses() > 0 && freshVoucher.getCurrentUses() >= freshVoucher.getMaxUses()) {
            freshVoucher.setStatus(VoucherStatus.APPLIED);
        }
        voucherRepository.save(freshVoucher);
    }

    /**
     * Cancel a pending voucher usage (called when payment fails / is retried).
     *
     * @param voucher The voucher
     * @param identity The patron's identity
     */
    @Transactional
    public void cancelPendingUsage(Voucher voucher, Identity identity) {
        Optional<VoucherUsage> usageOpt = voucherUsageRepository.findByVoucherAndIdentityAndConfirmedFalse(voucher, identity);
        usageOpt.ifPresent(usage -> voucherUsageRepository.delete(usage));
    }

    // ========== CRUD ==========

    @Transactional(readOnly = true)
    public Optional<Voucher> findById(Long id) {
        Optional<Voucher> voucherOpt = voucherRepository.findById(id);
        voucherOpt.ifPresent(v -> org.hibernate.Hibernate.initialize(v.getUsages()));
        return voucherOpt;
    }

    public Optional<Voucher> findByCode(String code) {
        return voucherRepository.findByCode(code.trim().toUpperCase());
    }

    @Transactional
    public int expireActiveVouchers() {
        return voucherRepository.expireActiveVouchers(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Page<Voucher> findVouchers(Pageable pageable, String voucherSearch, String patronId,
                                      Collection<VoucherStatus> statuses, VoucherType type, DiscountType discountType,
                                       LocalDateTime expiresFrom, LocalDateTime expiresTo, LocalDateTime firstUsedFrom, LocalDateTime firstUsedTo) {
        Page<Voucher> vouchers = hasText(patronId)
            ? voucherRepository.findVouchersByPatronId(pageable, voucherSearch, patronId.trim(), statuses, type, discountType,
                expiresFrom, expiresTo, firstUsedFrom, firstUsedTo)
            : voucherRepository.findVouchers(pageable, voucherSearch, statuses, type, discountType,
                expiresFrom, expiresTo, firstUsedFrom, firstUsedTo);
        if (vouchers.hasContent()) {
            List<Long> voucherIds = vouchers.getContent().stream()
                .map(Voucher::getId)
                .collect(Collectors.toList());
            voucherRepository.findAllWithUsagesByIdIn(voucherIds);
        }
        return vouchers;
    }

    @Transactional(readOnly = true)
    public List<VoucherPrintData> findVoucherPrintData(
        Pageable pageable,
        String voucherSearch,
        String patronId,
        Collection<VoucherStatus> statuses,
        VoucherType type,
        DiscountType discountType,
        LocalDateTime expiresFrom,
        LocalDateTime expiresTo,
        LocalDateTime firstUsedFrom,
        LocalDateTime firstUsedTo
    ) {
        if (hasText(patronId)) {
            return voucherRepository.findVoucherPrintDataByPatronId(pageable, voucherSearch, patronId.trim(), statuses,
                type, discountType, expiresFrom, expiresTo, firstUsedFrom, firstUsedTo);
        }
        return voucherRepository.findVoucherPrintData(pageable, voucherSearch, statuses, type, discountType,
            expiresFrom, expiresTo, firstUsedFrom, firstUsedTo);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Transactional(readOnly = true)
    public List<Voucher> findAllByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return voucherRepository.findAllById(ids);
    }

    @Transactional
    public Voucher save(Voucher voucher) {
        voucher.setCode(voucher.getCode().trim().toUpperCase());
        return voucherRepository.save(voucher);
    }

    @Transactional
    public void deactivate(Long id) {
        voucherRepository.findById(id).ifPresent(v -> {
            v.setStatus(VoucherStatus.INACTIVE);
            voucherRepository.save(v);
        });
    }

    @Transactional
    public int deactivateActiveVouchers(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        List<Voucher> vouchers = voucherRepository.findAllById(ids);
        int deactivatedCount = 0;
        for (Voucher voucher : vouchers) {
            if (voucher.getStatus() == VoucherStatus.ACTIVE) {
                voucher.setStatus(VoucherStatus.INACTIVE);
                voucherRepository.save(voucher);
                deactivatedCount++;
            }
        }
        return deactivatedCount;
    }

    @Transactional
    public void activate(Long id) {
        voucherRepository.findById(id).ifPresent(v -> {
            if (v.getStatus() == VoucherStatus.INACTIVE) {
                v.setStatus(VoucherStatus.ACTIVE);
                voucherRepository.save(v);
            }
        });
    }

    public List<VoucherUsage> getUsagesByVoucher(Voucher voucher) {
        return voucherUsageRepository.findByVoucher(voucher);
    }

    public List<VoucherUsage> getUsagesByIdentity(Identity identity) {
        return voucherUsageRepository.findByIdentityOrderByUsedAtDesc(identity);
    }

    public List<VoucherUsage> getPendingUsagesByIdentity(Identity identity) {
        return voucherUsageRepository.findByIdentityAndConfirmedFalseOrderByUsedAtDesc(identity);
    }

    // ========== Denormalized fields ==========

    /**
     * Update denormalized firstUsedAt field on the voucher
     * based on confirmed usages.
     */
    private void updateDenormalizedFields(Voucher voucher) {
        List<VoucherUsage> confirmedUsages = voucherUsageRepository.findByVoucherAndConfirmedTrue(voucher);

        // Update firstUsedAt
        if (voucher.getFirstUsedAt() == null && !confirmedUsages.isEmpty()) {
            confirmedUsages.stream()
                .map(VoucherUsage::getUsedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .ifPresent(voucher::setFirstUsedAt);
        }
    }

    // ========== Bulk generation ==========

    /**
     * Generate multiple voucher codes with the same configuration.
     *
     * @param count Number of vouchers to generate
     * @param discountType Discount type (PERCENTAGE or FIXED_CZK)
     * @param discountValue Discount value
     * @param maxUses Max uses per voucher (0 = unlimited)
     * @param expiresAt Optional expiration date
     * @param note Optional admin note
     * @param type Voucher type (DIGITAL or ANALOG)
     * @param recipientType Optional recipient type for Voucher printer rendering
     * @return List of created vouchers
     */
    @Transactional
    public List<Voucher> bulkGenerate(int count, DiscountType discountType, BigDecimal discountValue,
                                       int maxUses, LocalDateTime expiresAt, String note, VoucherType type,
                                       VoucherRecipientType recipientType, String buyer, String paymentMethod, String invoice) {
        List<Voucher> vouchers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String code = generateUniqueCode();
            Voucher voucher = new Voucher(code, discountType, discountValue, maxUses);
            voucher.setExpiresAt(expiresAt);
            voucher.setNote(note);
            voucher.setType(type != null ? type : VoucherType.DIGITAL);
            voucher.setRecipientType(recipientType);
            voucher.setBuyer(buyer);
            voucher.setPaymentMethod(paymentMethod);
            voucher.setInvoice(invoice);
            vouchers.add(voucherRepository.save(voucher));
        }
        return vouchers;
    }

    /**
     * Generate a unique voucher code
     */
    public String generateUniqueCode() {
        SecureRandom random = new SecureRandom();
        String code;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder(DEFAULT_CODE_LENGTH);
            for (int i = 0; i < DEFAULT_CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("Unable to generate unique voucher code after 100 attempts");
            }
        } while (voucherRepository.existsByCode(code));
        return code;
    }
}
