package cz.cas.lib.bankid_registrator.entities.voucher;

/**
 * DiscountType determines how the voucher discount value is interpreted.
 */
public enum DiscountType {
    /**
     * Discount value is a percentage (0–100) of the fee amount
     */
    PERCENTAGE,

    /**
     * Discount value is a fixed amount in CZK
     */
    FIXED_CZK
}
