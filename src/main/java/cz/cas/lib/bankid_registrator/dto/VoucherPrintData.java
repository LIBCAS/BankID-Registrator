package cz.cas.lib.bankid_registrator.dto;

import cz.cas.lib.bankid_registrator.entities.voucher.DiscountType;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherRecipientType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * Lightweight voucher data used when preparing Voucher printer batches.
 *
 * This deliberately excludes voucher usages and patron identities.
 */
@Getter
public class VoucherPrintData
{
    private final Long id;
    private final String code;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private final LocalDateTime firstUsedAt;
    private final DiscountType discountType;
    private final BigDecimal discountValue;
    private final VoucherRecipientType recipientType;

    public VoucherPrintData(
        Long id,
        String code,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime firstUsedAt,
        DiscountType discountType,
        BigDecimal discountValue,
        VoucherRecipientType recipientType
    ) {
        this.id = id;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.firstUsedAt = firstUsedAt;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.recipientType = recipientType;
    }
}
