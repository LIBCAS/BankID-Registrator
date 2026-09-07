package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.AlephServiceConfig;
import cz.cas.lib.bankid_registrator.configurations.PaymentServiceConfig;
import cz.cas.lib.bankid_registrator.dao.mariadb.PaymentRepository;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentStatus;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.payment.Payment;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Base64;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.ASN1Sequence;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService extends ServiceAbstract
{
    private final PaymentRepository paymentRepository;
    private final AlephService alephService;
    private final PaymentServiceConfig paymentServiceConfig;
    private final AlephServiceConfig alephServiceConfig;

    public PaymentService(PaymentRepository paymentRepository, AlephService alephService,
                         PaymentServiceConfig paymentServiceConfig, AlephServiceConfig alephServiceConfig) {
        super(null);
        this.paymentRepository = paymentRepository;
        this.alephService = alephService;
        this.paymentServiceConfig = paymentServiceConfig;
        this.alephServiceConfig = alephServiceConfig;
    }

    /**
     * Create a new payment record (without voucher)
     * @param identity - The identity associated with this payment
     * @param type - Payment type (REGISTRATION or RENEWAL)
     * @return Payment object
     */
    @Transactional
    public Payment createPayment(Identity identity, PaymentType type) {
        return createPayment(identity, type, null, BigDecimal.ZERO);
    }

    /**
     * Create a new payment record with optional voucher discount
     * @param identity - The identity associated with this payment
     * @param type - Payment type (REGISTRATION or RENEWAL)
     * @param voucherCode - Voucher code applied (null if no voucher)
     * @param discountAmount - Discount amount in CZK (BigDecimal.ZERO if no discount)
     * @return Payment object
     */
    @Transactional
    public Payment createPayment(Identity identity, PaymentType type, String voucherCode, BigDecimal discountAmount) {
        // Get the amount from Aleph (total due cash)
        BigDecimal amount = getPatronTotalDueCash(identity.getAlephId());

        // Create payment record with voucher info
        PaymentStatus status;
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0
                && amount.compareTo(BigDecimal.ZERO) == 0) {
            // No Aleph charge exists. Positive balances must be settled through the payments API,
            // even when the voucher reduces the amount payable to zero.
            status = PaymentStatus.VOUCHER_COVERED;
        } else {
            status = PaymentStatus.PENDING;
        }

        Payment payment = new Payment(identity, type, status, amount);
        payment.setVoucherCode(voucherCode);
        payment.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);

        return paymentRepository.save(payment);
    }

    /**
     * Get payment by Aleph barcode (e.g. used when Comgate redirects back)
     * @param alephBarcode - Aleph patron barcode
     * @return Optional<Payment>
     */
    public Optional<Payment> getPaymentByAlephBarcode(String alephBarcode) {
        return paymentRepository.findFirstByIdentity_AlephBarcodeOrderByCreatedAtDescIdDesc(alephBarcode);
    }

    /**
     * Get payment by ID
     * @param id - Payment ID
     * @return Optional<Payment>
     */
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    /**
     * Get all payments for an identity
     * @param identity - The identity
     * @return List of payments
     */
    public List<Payment> getPaymentsByIdentity(Identity identity) {
        return paymentRepository.findByIdentity(identity);
    }

    /**
     * Get the latest payment for an identity
     * @param identity - The identity
     * @return Optional<Payment>
     */
    public Optional<Payment> getLatestPaymentByIdentity(Identity identity) {
        return paymentRepository.findFirstByIdentityOrderByCreatedAtDescIdDesc(identity);
    }

    /**
     * Check whether this identity already has a payment for the supplied business operation
     */
    public boolean paymentExists(Identity identity, PaymentType type) {
        return paymentRepository.existsByIdentityAndType(identity, type);
    }

    /**
     * Update payment status
     * @param payment - The payment to update
     * @param status - New status
     */
    @Transactional
    public void updatePaymentStatus(Payment payment, PaymentStatus status) {
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    /**
     * Save payment
     * @param payment - The payment to save
     */
    @Transactional
    public void save(Payment payment) {
        paymentRepository.save(payment);
    }

    /**
     * Get patron's total outstanding cash fines from Aleph
     * @param patronId - Aleph patron ID
     * @return Total due cash amount
     */
    public BigDecimal getPatronTotalDueCash(String patronId) {
        Map<String, Object> finesResult = alephService.getPatronFines(patronId);

        if (finesResult == null || finesResult.containsKey("error")
                || !(finesResult.get("totalDueCash") instanceof BigDecimal)
                || ((BigDecimal) finesResult.get("totalDueCash")).signum() < 0) {
            throw new IllegalStateException("Aleph returned an invalid payment amount");
        }
        return (BigDecimal) finesResult.get("totalDueCash");
    }

    /**
     * Refresh a pending payment's total amount from Aleph.
     *
     * Aleph is the source of truth for the patron's current outstanding balance. The amount
     * persisted on Payment records is only a snapshot taken when the payment was created and
     * may become stale after an Aleph-side correction. Never retain the stored amount when the
     * Aleph lookup fails because doing so could present or sign an incorrect payment amount.
     *
     * @param payment - payment whose amount should match the current Aleph balance
     * @return the same payment instance, with its amount refreshed
     * @throws IllegalStateException when Aleph cannot provide a valid current balance
     */
    @Transactional
    public Payment refreshPaymentAmountFromAleph(Payment payment) {
        String patronId = payment.getIdentity().getAlephId();
        BigDecimal currentAmount = getPatronTotalDueCash(patronId);

        if (payment.getAmount().compareTo(currentAmount) != 0) {
            BigDecimal previousAmount = payment.getAmount();
            payment.setAmount(currentAmount);
            payment = paymentRepository.save(payment);
            getLogger().info("Refreshed payment {} amount from Aleph for patron {}: {} -> {}",
                payment.getId(), patronId, previousAmount, currentAmount);
        }

        return payment;
    }

    /**
     * Verify if a patron has paid (i.e., has no outstanding fines)
     * @param patronId - Aleph patron ID
     * @return true if paid (no outstanding fines), false otherwise
     */
    public boolean verifyPaymentStatus(String patronId) {
        BigDecimal totalDue = getPatronTotalDueCash(patronId);
        return totalDue.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Generate payment digest (RSA signature) for payment form submission to LIBCAS Payment Gateway API
     *
     * @param patronId - Aleph patron ID
     * @param amount - Payment amount
     * @param timestamp - Unix timestamp
     * @return Base64-encoded RSA signature
     */
    public String generatePaymentDigest(String patronId, BigDecimal amount, long timestamp) {
        try {
            // Format: timestamp|patronId|adm|amount
            // Amount must be in cents/halers to match what's sent in form data
            String admLibrary = alephServiceConfig.getAdmLibrary();
            int amountInCents = amount.multiply(new BigDecimal("100")).intValue();
            String plainText = timestamp + "|" + patronId + "|" + admLibrary + "|" + amountInCents;

            // Load private key
            PrivateKey privateKey = loadPrivateKey();

            // Sign with RSA SHA1
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            signature.update(plainText.getBytes("UTF-8"));
            byte[] signatureBytes = signature.sign();

            // Base64 encode
            return Base64.getEncoder().encodeToString(signatureBytes);

        } catch (Exception e) {
            getLogger().error("Failed to generate payment digest: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate payment digest", e);
        }
    }

    /**
     * Generate payment form data for submitting to LIBCAS Payment Gateway API.
     * The "amount" field is the post-discount amount (what the patron actually pays).
     * The "discount" field is sent separately for LIBCAS Payments Gateway API-side validation (amount + discount == alephTotal).
     * The RSA signature signs the post-discount amount — no signature format change needed.
     *
     * @param payment - The payment record
     * @param identity - The identity associated with the payment
     * @return Map with form field names and values
     */
    public Map<String, String> generatePaymentFormData(Payment payment, Identity identity) {
        return generatePaymentFormData(payment, identity, null);
    }

    /**
     * Generate payment form data for submitting to LIBCAS Payment Gateway API.
     * Optionally includes a return URL for zero-payment flows where LIBCAS Payments Gateway API
     * finalizes Aleph directly and must redirect the user back to the app.
     *
     * @param payment - The payment record
     * @param identity - The identity associated with the payment
     * @param returnUrl - Optional absolute return URL for zero-payment flows
     * @return Map with form field names and values
     */
    public Map<String, String> generatePaymentFormData(Payment payment, Identity identity, String returnUrl) {
        long timestamp = System.currentTimeMillis() / 1000; // Unix timestamp in seconds

        // Use post-discount amount for both signature and form field
        BigDecimal amountToPay = payment.getAmountToPay();
        String digest = generatePaymentDigest(identity.getAlephId(), amountToPay, timestamp);
        String admLibrary = alephServiceConfig.getAdmLibrary();

        // Convert amounts from CZK to cents/halers (multiply by 100) to match Aleph database format
        int amountInCents = amountToPay.multiply(new BigDecimal("100")).intValue();

        Map<String, String> formData = new HashMap<>();
        formData.put("id", identity.getAlephId());
        formData.put("adm", admLibrary);
        formData.put("amount", String.valueOf(amountInCents));
        formData.put("time", String.valueOf(timestamp));
        formData.put("digest", digest);

        // Include discount for LIBCAS Payments Gateway API-side validation (backward-compatible: absent = no discount)
        BigDecimal discount = payment.getDiscountAmount();
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            int discountInCents = discount.multiply(new BigDecimal("100")).intValue();
            formData.put("discount", String.valueOf(discountInCents));
        }

        if (returnUrl != null && !returnUrl.trim().isEmpty()) {
            formData.put("return_url", returnUrl);
        }

        return formData;
    }

    /**
     * Load the RSA private key from file (supports both PKCS#1 and PKCS#8 formats)
     * @return PrivateKey object
     */
    private PrivateKey loadPrivateKey() throws Exception {
        String keyPath = paymentServiceConfig.getPrivateKeyPath();
        File keyFile = new File(keyPath);

        if (!keyFile.exists()) {
            throw new RuntimeException("Private key file not found: " + keyPath);
        }

        String keyContent = new String(Files.readAllBytes(keyFile.toPath()));
        boolean isPKCS1 = keyContent.contains("BEGIN RSA PRIVATE KEY");

        // Remove header, footer, and whitespace
        keyContent = keyContent
            .replaceAll("-----BEGIN PRIVATE KEY-----", "")
            .replaceAll("-----END PRIVATE KEY-----", "")
            .replaceAll("-----BEGIN RSA PRIVATE KEY-----", "")
            .replaceAll("-----END RSA PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        if (isPKCS1) {
            // PKCS#1 format - need to parse using BouncyCastle and convert to PKCS#8
            ASN1Sequence sequence = ASN1Sequence.getInstance(keyBytes);
            RSAPrivateKey rsaPrivateKey = RSAPrivateKey.getInstance(sequence);

            RSAPrivateCrtKeySpec spec = new RSAPrivateCrtKeySpec(
                rsaPrivateKey.getModulus(),
                rsaPrivateKey.getPublicExponent(),
                rsaPrivateKey.getPrivateExponent(),
                rsaPrivateKey.getPrime1(),
                rsaPrivateKey.getPrime2(),
                rsaPrivateKey.getExponent1(),
                rsaPrivateKey.getExponent2(),
                rsaPrivateKey.getCoefficient()
            );

            return keyFactory.generatePrivate(spec);
        } else {
            // PKCS#8 format - use directly
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return keyFactory.generatePrivate(spec);
        }
    }
}
