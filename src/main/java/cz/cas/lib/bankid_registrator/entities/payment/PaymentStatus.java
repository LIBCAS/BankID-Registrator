package cz.cas.lib.bankid_registrator.entities.payment;

public enum PaymentStatus {
    PENDING,          // Waiting for the confirmation of payment by the bank
    SUCCESS,          // Payment completed successfully
    FAILED,           // Payment failed or cancelled by user
    SKIPPED,          // User chose to pay later
    VOUCHER_COVERED   // Fee fully covered by voucher (no payment gateway needed)
}
