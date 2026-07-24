package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.SessionTimerConfig;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentStatus;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.payment.Payment;
import cz.cas.lib.bankid_registrator.services.IdentityAuthService;
import cz.cas.lib.bankid_registrator.services.TokenService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Local Controller for testing payment page templates
 */
@Controller
@Profile("local")
@RequestMapping("/local/payment")
public class LocalPaymentController extends ControllerAbstract
{
    private final TokenService tokenService;

    public LocalPaymentController(
        MessageSource messageSource,
        IdentityAuthService identityAuthService,
        TokenService tokenService,
        SessionTimerConfig sessionTimerConfig
    ) {
        super(messageSource, identityAuthService, sessionTimerConfig);
        this.tokenService = tokenService;
    }

    /**
     * Test initial payment page - Registration flow
     */
    @RequestMapping(value="/initial/registration", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testPaymentInitial_Registration(Model model, Locale locale, HttpSession session) {
        // Create test identity
        Identity identity = createTestIdentity(1L, "KNAV12345", "2707012345", false);

        // Create test payment
        Payment payment = createTestPayment(1L, identity, PaymentType.REGISTRATION, PaymentStatus.PENDING, new BigDecimal("200.00"));

        // Initialize all flags
        initializeModelFlags(model);

        // Add model attributes
        model.addAttribute("payment", payment);
        model.addAttribute("identity", identity);
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("paymentDeadline", 7);
        model.addAttribute("paymentType", payment.getType());
        model.addAttribute("isRegistration", true);
        model.addAttribute("showPaymentForm", true);

        return "payment";
    }

    /**
     * Test initial payment page - Renewal flow
     */
    @RequestMapping(value="/initial/renewal", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testPaymentInitial_Renewal(Model model, Locale locale, HttpSession session) {
        // Create test identity
        Identity identity = createTestIdentity(2L, "KNAV54321", "2707054321", false);

        // Create test payment
        Payment payment = createTestPayment(2L, identity, PaymentType.RENEWAL, PaymentStatus.PENDING, new BigDecimal("150.00"));

        // Initialize all flags
        initializeModelFlags(model);

        // Add model attributes
        model.addAttribute("payment", payment);
        model.addAttribute("identity", identity);
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("paymentDeadline", 7);
        model.addAttribute("paymentType", payment.getType());
        model.addAttribute("isRegistration", false);
        model.addAttribute("showPaymentForm", true);

        return "payment";
    }

    /**
     * Test payment success - Registration flow (with progress loader)
     */
    @RequestMapping(value="/success/registration", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testPaymentSuccess_Registration(Model model, Locale locale, HttpSession session) {
        // Create test identity
        Identity identity = createTestIdentity(3L, "KNAV11111", "2707011111", false);

        // Create test payment
        Payment payment = createTestPayment(3L, identity, PaymentType.REGISTRATION, PaymentStatus.SUCCESS, new BigDecimal("200.00"));

        // Initialize all flags
        initializeModelFlags(model);

        // Add model attributes
        model.addAttribute("payment", payment);
        model.addAttribute("identity", identity);
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("paymentDeadline", 7);
        model.addAttribute("paymentType", payment.getType());
        model.addAttribute("isRegistration", true);
        model.addAttribute("paymentSuccess", true);
        model.addAttribute("showProgressLoader", true);
        model.addAttribute("successMessage", this.messageSource.getMessage("payment.success.registration", null, locale));
        model.addAttribute("apiToken", tokenService.createApiToken(identity.getId().toString()));

        return "payment";
    }

    /**
     * Test payment success - Renewal flow (no progress loader)
     */
    @RequestMapping(value="/success/renewal", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testPaymentSuccess_Renewal(Model model, Locale locale, HttpSession session) {
        // Create test identity
        Identity identity = createTestIdentity(4L, "KNAV22222", "2707022222", false);

        // Create test payment
        Payment payment = createTestPayment(4L, identity, PaymentType.RENEWAL, PaymentStatus.SUCCESS, new BigDecimal("150.00"));

        // Initialize all flags
        initializeModelFlags(model);

        // Add model attributes
        model.addAttribute("payment", payment);
        model.addAttribute("identity", identity);
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("paymentDeadline", 7);
        model.addAttribute("paymentType", payment.getType());
        model.addAttribute("isRegistration", false);
        model.addAttribute("paymentSuccess", true);
        model.addAttribute("showProgressLoader", false);
        model.addAttribute("successMessage", this.messageSource.getMessage("payment.success.renewal", null, locale));

        return "payment";
    }

    /**
     * Test payment failure - Registration flow
     */
    @RequestMapping(value="/failure/registration", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testPaymentFailure_Registration(Model model, Locale locale, HttpSession session) {
        // Create test identity
        Identity identity = createTestIdentity(5L, "KNAV33333", "2707033333", false);

        // Create test payment
        Payment payment = createTestPayment(5L, identity, PaymentType.REGISTRATION, PaymentStatus.FAILED, new BigDecimal("200.00"));

        // Initialize all flags
        initializeModelFlags(model);

        // Add model attributes
        model.addAttribute("payment", payment);
        model.addAttribute("identity", identity);
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("paymentDeadline", 7);
        model.addAttribute("paymentType", payment.getType());
        model.addAttribute("isRegistration", true);
        model.addAttribute("paymentFailed", true);
        model.addAttribute("showPaymentForm", true);
        model.addAttribute("errorMessage", this.messageSource.getMessage("payment.error.failed", null, locale));

        return "payment";
    }

    /**
     * Test payment failure - Renewal flow
     */
    @RequestMapping(value="/failure/renewal", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testPaymentFailure_Renewal(Model model, Locale locale, HttpSession session) {
        // Create test identity
        Identity identity = createTestIdentity(6L, "KNAV44444", "2707044444", false);

        // Create test payment
        Payment payment = createTestPayment(6L, identity, PaymentType.RENEWAL, PaymentStatus.FAILED, new BigDecimal("150.00"));

        // Initialize all flags
        initializeModelFlags(model);

        // Add model attributes
        model.addAttribute("payment", payment);
        model.addAttribute("identity", identity);
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("paymentDeadline", 7);
        model.addAttribute("paymentType", payment.getType());
        model.addAttribute("isRegistration", false);
        model.addAttribute("paymentFailed", true);
        model.addAttribute("showPaymentForm", true);
        model.addAttribute("errorMessage", this.messageSource.getMessage("payment.error.failed", null, locale));

        return "payment";
    }

    /**
     * Test payment skipped - Registration flow (with progress loader)
     */
    @RequestMapping(value="/skipped/registration", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testPaymentSkipped_Registration(Model model, Locale locale, HttpSession session) {
        // Create test identity
        Identity identity = createTestIdentity(7L, "KNAV55555", "2707055555", false);

        // Create test payment
        Payment payment = createTestPayment(7L, identity, PaymentType.REGISTRATION, PaymentStatus.SKIPPED, new BigDecimal("200.00"));

        // Initialize all flags
        initializeModelFlags(model);

        // Add model attributes
        model.addAttribute("payment", payment);
        model.addAttribute("identity", identity);
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("paymentDeadline", 7);
        model.addAttribute("paymentType", payment.getType());
        model.addAttribute("isRegistration", true);
        model.addAttribute("paymentSkipped", true);
        model.addAttribute("showProgressLoader", true);
        model.addAttribute("skippedMessage", this.messageSource.getMessage("payment.skipped.registration", null, locale));
        model.addAttribute("apiToken", tokenService.createApiToken(identity.getId().toString()));

        return "payment";
    }

    /**
     * Test payment skipped - Renewal flow (no progress loader)
     */
    @RequestMapping(value="/skipped/renewal", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testPaymentSkipped_Renewal(Model model, Locale locale, HttpSession session) {
        // Create test identity
        Identity identity = createTestIdentity(8L, "KNAV66666", "2707066666", false);

        // Create test payment
        Payment payment = createTestPayment(8L, identity, PaymentType.RENEWAL, PaymentStatus.SKIPPED, new BigDecimal("150.00"));

        // Initialize all flags
        initializeModelFlags(model);

        // Add model attributes
        model.addAttribute("payment", payment);
        model.addAttribute("identity", identity);
        model.addAttribute("amount", payment.getAmount());
        model.addAttribute("paymentDeadline", 7);
        model.addAttribute("paymentType", payment.getType());
        model.addAttribute("isRegistration", false);
        model.addAttribute("paymentSkipped", true);
        model.addAttribute("showProgressLoader", false);
        model.addAttribute("skippedMessage", this.messageSource.getMessage("payment.skipped.renewal", null, locale));

        return "payment";
    }

    /**
     * Helper method to initialize all boolean flags to false
     */
    private void initializeModelFlags(Model model) {
        model.addAttribute("showPaymentForm", false);
        model.addAttribute("paymentSuccess", false);
        model.addAttribute("paymentFailed", false);
        model.addAttribute("paymentSkipped", false);
        model.addAttribute("showProgressLoader", false);
    }

    /**
     * Helper method to create a test Identity
     */
    private Identity createTestIdentity(Long id, String alephId, String alephBarcode, boolean isCasEmployee) {
        Identity identity = new Identity();
        identity.setId(id);
        identity.setAlephId(alephId);
        identity.setAlephBarcode(alephBarcode);
        identity.setIsCasEmployee(isCasEmployee);
        identity.setBankId("test-bank-id-" + id);
        return identity;
    }

    /**
     * Helper method to create a test Payment
     */
    private Payment createTestPayment(Long id, Identity identity, PaymentType type, PaymentStatus status, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setIdentity(identity);
        payment.setType(type);
        payment.setStatus(status);
        payment.setAmount(amount);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }
}
