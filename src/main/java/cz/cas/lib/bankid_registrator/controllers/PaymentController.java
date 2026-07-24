package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.PaymentServiceConfig;
import cz.cas.lib.bankid_registrator.configurations.RegistrationFeeConfig;
import cz.cas.lib.bankid_registrator.configurations.SessionTimerConfig;
import cz.cas.lib.bankid_registrator.entities.payment.ComgateReturnStatus;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentStatus;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.payment.Payment;
import cz.cas.lib.bankid_registrator.services.IdentityAuthService;
import cz.cas.lib.bankid_registrator.services.IdentityService;
import cz.cas.lib.bankid_registrator.services.PaymentService;
import cz.cas.lib.bankid_registrator.services.TokenService;
import cz.cas.lib.bankid_registrator.services.VoucherService;
import cz.cas.lib.bankid_registrator.util.WebUtils;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payment")
public class PaymentController extends ControllerAbstract
{
    private final PaymentService paymentService;
    private final PaymentServiceConfig paymentServiceConfig;
    private final RegistrationFeeConfig registrationFeeConfig;
    private final TokenService tokenService;
    private final IdentityService identityService;
    private final VoucherService voucherService;

    public PaymentController(
        MessageSource messageSource,
        IdentityAuthService identityAuthService,
        PaymentService paymentService,
        PaymentServiceConfig paymentServiceConfig,
        RegistrationFeeConfig registrationFeeConfig,
        TokenService tokenService,
        IdentityService identityService,
        VoucherService voucherService,
        SessionTimerConfig sessionTimerConfig
    ) {
        super(messageSource, identityAuthService, sessionTimerConfig);
        this.paymentService = paymentService;
        this.paymentServiceConfig = paymentServiceConfig;
        this.registrationFeeConfig = registrationFeeConfig;
        this.tokenService = tokenService;
        this.identityService = identityService;
        this.voucherService = voucherService;
    }

    /**
     * Payment callback from Comgate (user redirected back after payment)
     * GET /payment/callback?refId={patronBarcode}&id={comgateId}&status={status}
     */
    @GetMapping("/callback")
    public String paymentCallback(
        @RequestParam String refId,  // Patron barcode from Comgate
        @RequestParam(required = false) String id,  // Comgate transaction ID
        @RequestParam(required = false) String status,
        Model model,
        Locale locale,
        HttpServletRequest request
    ) {
        // Get payment by patron barcode (refId from Comgate)
        Optional<Payment> paymentOpt = paymentService.getPaymentByAlephBarcode(refId);

        if (!paymentOpt.isPresent()) {
            getLogger().error("Payment not found for barcode: {}", refId);
            return "redirect:/error";
        }

        Payment payment = paymentOpt.get();
        Identity identity = payment.getIdentity();
        Optional<ComgateReturnStatus> callbackStatus = ComgateReturnStatus.fromValue(status);

        boolean isAuthenticatedForPayment = isAuthenticatedAs(request, identity);

        if (!isAuthenticatedForPayment && callbackStatus.orElse(null) == ComgateReturnStatus.CANCELLED) {
            getLogger().info("Rendering public cancelled payment return page for logged-out user. Barcode: {}, Status: {}",
                refId, status);
            return handlePublicCancelledReturn(payment, model, locale);
        }

        if (!isAuthenticatedForPayment && callbackStatus.orElse(null) == ComgateReturnStatus.PENDING) {
            getLogger().info("Redirecting logged-out pending payment return to welcome page. Barcode: {}", refId);
            return "redirect:/welcome";
        }

        // Check if user is authenticated and matches the payment identity
        if (!isAuthenticatedForPayment) {
            getLogger().warn("Unauthorized access attempt to payment page. Barcode: {}", refId);
            return "redirect:/error";
        }

        // Add payment data to model
        model.addAttribute("payment", payment);
        model.addAttribute("identity", identity);
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("paymentDeadline", paymentServiceConfig.getPaymentDeadlineDays());
        model.addAttribute("paymentType", payment.getType());
        model.addAttribute("isRegistration", payment.getType() == PaymentType.REGISTRATION);
        model.addAttribute("isFinesOnly", payment.getType() == PaymentType.RENEWAL_FINES_ONLY);
        model.addAttribute("allowVoucherOnPaymentPage", payment.getType() != PaymentType.RENEWAL_FINES_ONLY);

        // Add discount info
        BigDecimal discountAmount = payment.getDiscountAmount();
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("amountToPay", payment.getAmountToPay());
        model.addAttribute("voucherCode", payment.getVoucherCode());
        model.addAttribute("paymentCoveredByVoucher", isZeroPaymentFinalizeViaPaymentsApi(payment));

        // Initialize all boolean flags to false (will be set to true in specific handlers)
        model.addAttribute("showPaymentForm", false);
        model.addAttribute("paymentSuccess", false);
        model.addAttribute("paymentFailed", false);
        model.addAttribute("paymentSkipped", false);
        model.addAttribute("publicPaymentCancelled", false);
        model.addAttribute("showProgressLoader", false);

        // Handle different status scenarios
        if (callbackStatus.isPresent()) {
            switch (callbackStatus.get()) {
                case SUCCESS:
                    return handlePaymentSuccess(payment, identity, model, locale, request);

                case CANCELLED:
                    return handlePaymentFailure(payment, identity, model, locale);

                case PENDING:
                    model.addAttribute("statusMessage", getMessage("payment.status.pending", locale));
                    break;
            }
        }

        // Initial state - show payment buttons
        setPageTitle(model, locale, payment.getType() == PaymentType.RENEWAL_FINES_ONLY
            ? "page.payment.title.fines"
            : "page.payment.title");
        model.addAttribute("showPaymentForm", true);
        return "payment";
    }

    /**
     * Show initial payment page (before going to LIBCAS Payment Gateway API)
     * GET /payment
     */
    @GetMapping("")
    public String paymentPage(
        Model model,
        Locale locale,
        HttpServletRequest request
    ) {
        // User must be authenticated
        if (!identityAuthService.isLoggedin(request)) {
            return "redirect:/error";
        }

        // Get identity from session
        Long identityId = identityAuthService.getAuthenticatedIdentityId(request);
        Optional<Identity> identityOpt = identityService.findById(identityId);

        if (!identityOpt.isPresent()) {
            getLogger().error("Identity not found for ID: {}", identityId);
            return "redirect:/error";
        }

        Identity identity = identityOpt.get();

        // Get latest payment for this identity
        Optional<Payment> paymentOpt = paymentService.getLatestPaymentByIdentity(identity);

        if (!paymentOpt.isPresent()) {
            getLogger().error("No payment found for identity: {}", identityId);
            return "redirect:/error";
        }

        Payment payment = paymentOpt.get();

        // Add payment data to model
        model.addAttribute("payment", payment);
        model.addAttribute("identity", identity);
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("paymentDeadline", paymentServiceConfig.getPaymentDeadlineDays());
        model.addAttribute("paymentType", payment.getType());
        model.addAttribute("isRegistration", payment.getType() == PaymentType.REGISTRATION);
        model.addAttribute("isFinesOnly", payment.getType() == PaymentType.RENEWAL_FINES_ONLY);
        model.addAttribute("allowVoucherOnPaymentPage", payment.getType() != PaymentType.RENEWAL_FINES_ONLY);

        // Add discount info
        BigDecimal discountAmount = payment.getDiscountAmount();
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("amountToPay", payment.getAmountToPay());
        model.addAttribute("voucherCode", payment.getVoucherCode());
        model.addAttribute("paymentCoveredByVoucher", isZeroPaymentFinalizeViaPaymentsApi(payment));

        // Initialize all boolean flags
        initializeModelFlags(model);

        // Show payment form
        setPageTitle(model, locale, payment.getType() == PaymentType.RENEWAL_FINES_ONLY
            ? "page.payment.title.fines"
            : "page.payment.title");
        model.addAttribute("showPaymentForm", true);
        return "payment";
    }

    /**
     * Initiate payment - show form that auto-submits to LIBCAS Payment Gateway API
     * POST /payment/initiate
     */
    @RequestMapping(value = "/initiate", method = {RequestMethod.GET, RequestMethod.POST})
    public String initiatePayment(
        Model model,
        HttpServletRequest request
    ) {
        // User must be authenticated
        if (!identityAuthService.isLoggedin(request)) {
            return "redirect:/error";
        }

        Long identityId = identityAuthService.getAuthenticatedIdentityId(request);
        Optional<Identity> identityOpt = identityService.findById(identityId);

        if (!identityOpt.isPresent()) {
            return "redirect:/error";
        }

        Identity identity = identityOpt.get();

        // Get latest payment
        Optional<Payment> paymentOpt = paymentService.getLatestPaymentByIdentity(identity);

        if (!paymentOpt.isPresent()) {
            getLogger().error("No payment found for identity: {}", identityId);
            return "redirect:/error";
        }

        Payment payment = paymentOpt.get();

        // If fee is fully covered by voucher, skip payment gateway entirely
        if (payment.getStatus() == PaymentStatus.VOUCHER_COVERED) {
            getLogger().info("Payment fully covered by voucher. Skipping payment gateway. Identity: {}", identityId);
            return "redirect:/payment/callback?refId=" + identity.getAlephBarcode() + "&status=success";
        }

        String returnUrl = null;
        if (isZeroPaymentFinalizeViaPaymentsApi(payment)) {
            returnUrl = buildPaymentSuccessReturnUrl(request, identity);
        }

        // Generate payment form data (with digest)
        Map<String, String> formData = paymentService.generatePaymentFormData(payment, identity, returnUrl);

        // Add form data and API URL to model
        model.addAttribute("paymentApiUrl", paymentServiceConfig.getApiUrl());
        model.addAttribute("formData", formData);

        getLogger().info("Initiating payment. Identity ID: {}, Form Data: {}", identityId, formData);

        // Return template that auto-submits the form to LIBCAS Payment Gateway API
        return "payment_redirect";
    }

    /**
     * Apply a voucher code to an existing payment.
     * Used when a returning user wants to apply a voucher on the payment page.
     * POST /payment/apply-voucher
     */
    @PostMapping("/apply-voucher")
    public String applyVoucher(
        @RequestParam(value = "voucherCode", required = false) String voucherCode,
        HttpServletRequest request,
        Locale locale,
        RedirectAttributes redirectAttributes
    ) {
        // User must be authenticated
        if (!identityAuthService.isLoggedin(request)) {
            return "redirect:/error";
        }

        Long identityId = identityAuthService.getAuthenticatedIdentityId(request);
        Optional<Identity> identityOpt = identityService.findById(identityId);

        if (!identityOpt.isPresent()) {
            return "redirect:/error";
        }

        Identity identity = identityOpt.get();

        // Get latest payment
        Optional<Payment> paymentOpt = paymentService.getLatestPaymentByIdentity(identity);

        if (!paymentOpt.isPresent()) {
            return "redirect:/error";
        }

        Payment payment = paymentOpt.get();

        if (payment.getType() == PaymentType.RENEWAL_FINES_ONLY) {
            redirectAttributes.addFlashAttribute("voucherError", getMessage("voucher.error.notApplicableToFines", locale));
            return "redirect:/payment";
        }

        // Validate the voucher code
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("voucherError", getMessage("voucher.error.empty", locale));
            return "redirect:/payment";
        }

        BigDecimal feeAmount = registrationFeeConfig.getDefaultAmount();
        Map<String, Object> voucherResult = voucherService.validateVoucher(voucherCode.trim(), identity, feeAmount);

        if (!Boolean.TRUE.equals(voucherResult.get("valid"))) {
            String errorKey = (String) voucherResult.getOrDefault("error", "voucher.error.invalid");
            redirectAttributes.addFlashAttribute("voucherError", getMessage(errorKey, locale));
            return "redirect:/payment";
        }

        BigDecimal discountAmount = (BigDecimal) voucherResult.get("discountAmount");
        String appliedVoucherCode = voucherCode.trim().toUpperCase();

        // Update the existing payment with voucher info
        payment.setVoucherCode(appliedVoucherCode);
        payment.setDiscountAmount(discountAmount);

        // Check if voucher fully covers the fee
        if (payment.getAmountToPay().compareTo(BigDecimal.ZERO) <= 0) {
            paymentService.save(payment);

            // Keep this as a normal payment retry path so LIBCAS Payments Gateway API can finalize Aleph
            // and attempt to cancel the stale Comgate transaction from the first attempt after user confirmation.
            voucherService.createPendingUsage(
                voucherService.findByCode(appliedVoucherCode).orElse(null),
                identity, discountAmount);

            getLogger().info("Voucher '{}' fully covers fee on payment page. Waiting for user confirmation before zero-payment finalization. Identity: {}",
                appliedVoucherCode, identity.getId());
            return "redirect:/payment";
        }

        // Partial discount — save payment and create pending usage
        paymentService.save(payment);
        voucherService.createPendingUsage(
            voucherService.findByCode(appliedVoucherCode).orElse(null),
            identity, discountAmount);

        getLogger().info("Voucher '{}' applied or replaced (partial discount: {}). Identity: {}",
            appliedVoucherCode, discountAmount, identity.getId());

        return "redirect:/payment";
    }

    /**
     * Skip payment - user chooses to pay later
     * POST /payment/skip
     */
    /*
    @PostMapping("/skip")
    public String skipPayment(
        HttpServletRequest request
    ) {
        // User must be authenticated
        if (!identityAuthService.isLoggedin(request)) {
            return "redirect:/error";
        }

        Long identityId = identityAuthService.getAuthenticatedIdentityId(request);
        Optional<Identity> identityOpt = identityService.findById(identityId);

        if (!identityOpt.isPresent()) {
            return "redirect:/error";
        }

        Identity identity = identityOpt.get();

        // Get latest payment
        Optional<Payment> paymentOpt = paymentService.getLatestPaymentByIdentity(identity);

        if (!paymentOpt.isPresent()) {
            getLogger().error("No payment found for identity: {}", identityId);
            return "redirect:/error";
        }

        Payment payment = paymentOpt.get();

        // Update payment status to SKIPPED
        paymentService.updatePaymentStatus(payment, PaymentStatus.SKIPPED);

        getLogger().info("Payment skipped. Barcode: {}, Identity: {}", identity.getAlephBarcode(), identity.getId());

        // Redirect back to payment callback with skipped status
        return "redirect:/payment/callback?refId=" + identity.getAlephBarcode() + "&status=skipped";
    }
    */

    /**
     * Handle successful payment callback
     */
    private String handlePaymentSuccess(Payment payment, Identity identity, Model model, Locale locale, HttpServletRequest request) {
        boolean isPaid;

        if (payment.getStatus() == PaymentStatus.VOUCHER_COVERED) {
            // Fee was fully covered by voucher — no need to verify with Aleph
            isPaid = true;
            getLogger().info("Payment fully covered by voucher. Skipping Aleph verification. Identity: {}", identity.getId());
        } else {
            // Verify payment with Aleph - don't trust URL parameter alone!
            isPaid = paymentService.verifyPaymentStatus(identity.getAlephId());
        }

        if (isPaid) {
            // Update payment status
            if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.VOUCHER_COVERED) {
                paymentService.updatePaymentStatus(payment, PaymentStatus.SUCCESS);
                getLogger().info("Payment verified and marked as SUCCESS. Identity: {}, Barcode: {}",
                    identity.getId(), identity.getAlephBarcode());
            }

            // Confirm voucher usage if a voucher was used (for partial discount payments verified by Aleph)
            if (payment.getVoucherCode() != null && !payment.getVoucherCode().isEmpty()) {
                voucherService.findByCode(payment.getVoucherCode()).ifPresent(voucher ->
                    voucherService.confirmOrCreateUsage(voucher, identity, payment.getDiscountAmount()));
            }

            model.addAttribute("paymentSuccess", true);
            boolean showProgressLoader = payment.getType() == PaymentType.REGISTRATION;
            model.addAttribute("showProgressLoader", showProgressLoader);

            // Add API token for progress loader (registration only)
            if (showProgressLoader) {
                model.addAttribute("apiToken", tokenService.createApiToken(identity.getId().toString()));
            } else {
                model.addAttribute("apiToken", tokenService.createApiToken(identity.getId().toString()));
                model.addAttribute("autoLogoutOnLoad", true);
                model.addAttribute("isIdentityLoggedIn", false);
            }

            // Success message
            String messageKey = payment.getType() == PaymentType.REGISTRATION
                ? "payment.success.registration"
                : payment.getType() == PaymentType.RENEWAL_FINES_ONLY
                    ? "payment.success.fines"
                    : "payment.success.renewal";
            model.addAttribute("successMessage", getMessage(messageKey, locale));
            setPageTitle(model, locale, payment.getType() == PaymentType.RENEWAL_FINES_ONLY
                ? "page.payment.success.title.fines"
                : "page.payment.success.title");

            return "payment";
        } else {
            // Payment verification failed - patron still has outstanding fines
            getLogger().warn("Payment verification failed. Identity: {}, PatronId: {}",
                identity.getId(), identity.getAlephId());

            // Update to FAILED status
            paymentService.updatePaymentStatus(payment, PaymentStatus.FAILED);

            return handlePaymentFailure(payment, identity, model, locale);
        }
    }

    /**
     * Handle failed or cancelled payment
     */
    private String handlePaymentFailure(Payment payment, Identity identity, Model model, Locale locale) {
        // Do NOT cancel pending voucher usage here — the failure may be a temporary
        // race condition (Comgate callback hasn't finalized Z31 yet). The pending usage
        // will be confirmed on the next successful verification, or cleaned up if the
        // user retries with a different voucher (createPendingUsage removes old pending).

        model.addAttribute("paymentFailed", true);
        model.addAttribute("showPaymentForm", true);
        String errorMessageKey = payment.getType() == PaymentType.RENEWAL_FINES_ONLY
            ? "payment.error.failed.fines"
            : "payment.error.failed";
        model.addAttribute("errorMessage", getMessage(errorMessageKey, locale));
        setPageTitle(model, locale, payment.getType() == PaymentType.RENEWAL_FINES_ONLY
            ? "page.payment.notPaid.title.fines"
            : "page.payment.notPaid.title");

        return "payment";
    }

    /**
     * Handle cancelled payment return from Comgate for a logged-out user.
     * Intentionally public-safe and contains no personal data.
     */
    private String handlePublicCancelledReturn(Payment payment, Model model, Locale locale) {
        initializeModelFlags(model);
        model.addAttribute("publicPaymentCancelled", true);
        model.addAttribute("paymentDeadline", paymentServiceConfig.getPaymentDeadlineDays());
        model.addAttribute("isRegistration", payment.getType() == PaymentType.REGISTRATION);
        model.addAttribute("isFinesOnly", payment.getType() == PaymentType.RENEWAL_FINES_ONLY);

        setPageTitle(model, locale, payment.getType() == PaymentType.RENEWAL_FINES_ONLY
            ? "page.payment.publicCancelled.title.fines"
            : "page.payment.publicCancelled.title");

        return "payment";
    }

    // /**
    //  * Handle skipped payment
    //  */
    // private String handlePaymentSkipped(Payment payment, Identity identity, Model model, Locale locale, HttpServletRequest request) {
    //     model.addAttribute("paymentSkipped", true);
    //     boolean showProgressLoader = payment.getType() == PaymentType.REGISTRATION;
    //     model.addAttribute("showProgressLoader", showProgressLoader);

    //     // Add API token for progress loader (registration only)
    //     if (showProgressLoader) {
    //         model.addAttribute("apiToken", tokenService.createApiToken(identity.getId().toString()));
    //     } else {
    //         model.addAttribute("apiToken", tokenService.createApiToken(identity.getId().toString()));
    //         model.addAttribute("autoLogoutOnLoad", true);
    //         model.addAttribute("isIdentityLoggedIn", false);
    //     }

    //     String messageKey = payment.getType() == PaymentType.REGISTRATION
    //         ? "payment.skipped.registration"
    //         : payment.getType() == PaymentType.RENEWAL_FINES_ONLY
    //             ? "payment.skipped.fines"
    //             : "payment.skipped.renewal";
    //     model.addAttribute("skippedMessage", getMessage(messageKey, locale));
    //     setPageTitle(model, locale, payment.getType() == PaymentType.RENEWAL_FINES_ONLY
    //         ? "page.payment.skipped.title.fines"
    //         : "page.payment.skipped.title");

    //     return "payment";
    // }

    /**
     * Initialize all boolean flags to false
     */
    private void initializeModelFlags(Model model) {
        model.addAttribute("showPaymentForm", false);
        model.addAttribute("paymentSuccess", false);
        model.addAttribute("paymentFailed", false);
        model.addAttribute("paymentSkipped", false);
        model.addAttribute("publicPaymentCancelled", false);
        model.addAttribute("showProgressLoader", false);
    }

    private boolean isZeroPaymentFinalizeViaPaymentsApi(Payment payment) {
        return payment.getAmountToPay().compareTo(BigDecimal.ZERO) == 0
            && payment.getDiscountAmount() != null
            && payment.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    private String buildPaymentSuccessReturnUrl(HttpServletRequest request, Identity identity) {
        return WebUtils.getBaseUrl(request)
            + "/payment/callback?refId="
            + identity.getAlephBarcode()
            + "&status=success";
    }

    private void setPageTitle(Model model, Locale locale, String messageKey) {
        model.addAttribute("pageTitle", getMessage(messageKey, locale));
    }

    /**
     * Check if the current user is authenticated as the given identity
     */
    private boolean isAuthenticatedAs(HttpServletRequest request, Identity identity) {
        return this.identityAuthService.isAuthenticatedAs(request, identity.getId());
    }

    /**
     * Get translated message
     */
    private String getMessage(String key, Locale locale) {
        return this.messageSource.getMessage(key, null, key, locale);
    }
}
