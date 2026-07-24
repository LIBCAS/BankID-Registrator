package cz.cas.lib.bankid_registrator.entities.voucher;

/**
 * VoucherStatus represents the current state of a voucher.
 */
public enum VoucherStatus {
    /**
     * Voucher is active and can be applied
     */
    ACTIVE,

    /**
     * Voucher was manually deactivated by an admin
     */
    INACTIVE,

    /**
     * Voucher has expired (past its expiresAt date)
     */
    EXPIRED,

    /**
     * Voucher has been fully used (currentUses >= maxUses)
     */
    APPLIED
}
