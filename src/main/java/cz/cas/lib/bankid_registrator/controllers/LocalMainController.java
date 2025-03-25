package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.dto.PatronPasswordDTO;
import cz.cas.lib.bankid_registrator.entities.patron.PatronLanguage;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.IdentityAuthService;
import cz.cas.lib.bankid_registrator.services.PatronService;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.util.Locale;
import javax.servlet.http.HttpSession;

@Controller
@Profile("local")
@RequestMapping("/local")
public class LocalMainController extends ControllerAbstract
{
    private final AlephService alephService;
    private final PatronService patronService;

    public LocalMainController(
        MessageSource messageSource,
        AlephService alephService,
        PatronService patronService,
        IdentityAuthService identityAuthService
    ) {
        super(messageSource, identityAuthService);
        this.alephService = alephService;
        this.patronService = patronService;

        init();
    }

    @RequestMapping(value = "/callback_registration_new/{patronAlephId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testViewCallbackRegistrationNew(
        @PathVariable("patronAlephId") String patronAlephId, 
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        Patron bankIdPatron = (Patron) this.alephService.getAlephPatron(patronAlephId, true).get("patron");
        PatronDTO bankIdPatronDTO = this.patronService.getPatronDTO(bankIdPatron);

        bankIdPatronDTO.setBirthDate(this.convertAlephPatronBirthdateForView(bankIdPatron.getBirthDate()));

        // Setting bankIdPatronDTO's conLng to the current locale
        bankIdPatronDTO.setConLng(locale.getLanguage().equals("en") ? PatronLanguage.ENG : PatronLanguage.CZE);

        model.addAttribute("pageTitle", this.messageSource.getMessage("page.welcome.title", null, locale));
        model.addAttribute("patronId", 1);
        model.addAttribute("patron", bankIdPatronDTO);

        return "callback_registration_new";
    }

    @RequestMapping(value = "/callback_registration_renewal/{patronAlephId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testViewCallbackRegistrationRenewal(
        @PathVariable("patronAlephId") String patronAlephId, 
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        Patron bankIdPatron = (Patron) this.alephService.getAlephPatron(patronAlephId, true).get("patron");
        PatronDTO bankIdPatronDTO = this.patronService.getPatronDTO(bankIdPatron);

        bankIdPatronDTO.setBirthDate(this.convertAlephPatronBirthdateForView(bankIdPatron.getBirthDate()));

        // Setting bankIdPatronDTO's conLng to the current locale
        bankIdPatronDTO.setConLng(locale.getLanguage().equals("en") ? PatronLanguage.ENG : PatronLanguage.CZE);

        String alephPatronExpiryDate = bankIdPatron.getExpiryDate();
        boolean membershipHasExpired = DateUtils.isDateExpired(alephPatronExpiryDate, "dd/MM/yyyy");
        boolean membershipExpiresToday = DateUtils.isDateToday(alephPatronExpiryDate, "dd/MM/yyyy");
        boolean expiryDateIn1MonthOrLess = DateUtils.isLessThanOrEqualToOneMonthFromToday(alephPatronExpiryDate, "dd/MM/yyyy");

        model.addAttribute("patronId", bankIdPatron.getSysId());
        model.addAttribute("patron", bankIdPatronDTO);
        model.addAttribute("bankIdPatron", bankIdPatronDTO);
        model.addAttribute("alephPatron", bankIdPatronDTO);
        model.addAttribute("membershipExpiryDate", alephPatronExpiryDate);
        model.addAttribute("membershipHasExpired", membershipHasExpired);
        model.addAttribute("membershipExpiresToday", membershipExpiresToday);
        model.addAttribute("expiryDateIn1MonthOrLess", expiryDateIn1MonthOrLess);

        return "callback_registration_renewal";
    }

    @RequestMapping(value = "/callback_registration_renewal/expires-now/{patronAlephId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testViewCallbackRegistrationRenewalExpiresNow(
        @PathVariable("patronAlephId") String patronAlephId, 
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        Patron bankIdPatron = (Patron) this.alephService.getAlephPatron(patronAlephId, true).get("patron");
        PatronDTO bankIdPatronDTO = this.patronService.getPatronDTO(bankIdPatron);

        bankIdPatronDTO.setBirthDate(this.convertAlephPatronBirthdateForView(bankIdPatron.getBirthDate()));

        // Setting bankIdPatronDTO's conLng to the current locale
        bankIdPatronDTO.setConLng(locale.getLanguage().equals("en") ? PatronLanguage.ENG : PatronLanguage.CZE);

        String alephPatronExpiryDate = DateUtils.getLastDateOfCurrentMonth("dd/MM/yyyy");
        boolean membershipHasExpired = DateUtils.isDateExpired(alephPatronExpiryDate, "dd/MM/yyyy");
        boolean membershipExpiresToday = DateUtils.isDateToday(alephPatronExpiryDate, "dd/MM/yyyy");
        boolean expiryDateIn1MonthOrLess = DateUtils.isLessThanOrEqualToOneMonthFromToday(alephPatronExpiryDate, "dd/MM/yyyy");

        model.addAttribute("patronId", bankIdPatron.getSysId());
        model.addAttribute("patron", bankIdPatronDTO);
        model.addAttribute("bankIdPatron", bankIdPatronDTO);
        model.addAttribute("alephPatron", bankIdPatronDTO);
        model.addAttribute("membershipExpiryDate", alephPatronExpiryDate);
        model.addAttribute("membershipHasExpired", membershipHasExpired);
        model.addAttribute("membershipExpiresToday", membershipExpiresToday);
        model.addAttribute("expiryDateIn1MonthOrLess", expiryDateIn1MonthOrLess);

        return "callback_registration_renewal";
    }

    @RequestMapping(value = "/new_registration_success/normal/email", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testViewNewRegistrationSuccess_normalWithEmail(
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        model.addAttribute("patronIsCasEmployee", false);
        model.addAttribute("patronHasEmail", true);
        model.addAttribute("membershipExpiryDate", DateUtils.addDaysToToday(365, "dd/MM/yyyy"));
        model.addAttribute("alephBarcode", "123456789");
        model.addAttribute("token", "myexampletoken");
        model.addAttribute("passwordDTO", new PatronPasswordDTO());

        return "new_registration_success";
    }

    @RequestMapping(value = "/new_registration_success/employee/email", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testViewNewRegistrationSuccess_employeeWithEmail(
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        model.addAttribute("patronIsCasEmployee", true);
        model.addAttribute("patronHasEmail", true);
        model.addAttribute("membershipExpiryDate", DateUtils.addDaysToToday(365, "dd/MM/yyyy"));
        model.addAttribute("alephBarcode", "123456789");
        model.addAttribute("token", "myexampletoken");
        model.addAttribute("passwordDTO", new PatronPasswordDTO());

        return "new_registration_success";
    }

    @RequestMapping(value = "/new_registration_success/normal/no-email", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testViewNewRegistrationSuccess_normalNoEmail(
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        model.addAttribute("patronIsCasEmployee", false);
        model.addAttribute("patronHasEmail", false);
        model.addAttribute("membershipExpiryDate", DateUtils.addDaysToToday(365, "dd/MM/yyyy"));
        model.addAttribute("alephBarcode", "123456789");
        model.addAttribute("token", "myexampletoken");
        model.addAttribute("passwordDTO", new PatronPasswordDTO());

        return "new_registration_success";
    }

    @RequestMapping(value = "/new_registration_success/employee/no-email", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testViewNewRegistrationSuccess_employeeNoEmail(
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        model.addAttribute("patronIsCasEmployee", true);
        model.addAttribute("patronHasEmail", false);
        model.addAttribute("membershipExpiryDate", DateUtils.addDaysToToday(365, "dd/MM/yyyy"));
        model.addAttribute("alephBarcode", "123456789");
        model.addAttribute("token", "myexampletoken");
        model.addAttribute("passwordDTO", new PatronPasswordDTO());

        return "new_registration_success";
    }

    @RequestMapping(value = "/membership_renewal_success/normal/email", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testViewMembershipRenewalSuccess_normalWithEmail(
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        model.addAttribute("isIdentityLoggedIn", false);
        model.addAttribute("patronIsCasEmployee", false);
        model.addAttribute("patronHasEmail", true);
        model.addAttribute("membershipExpiryDate", DateUtils.addDaysToToday(365, "dd/MM/yyyy"));
        model.addAttribute("alephBarcode", "123456789");

        return "membership_renewal_success";
    }

    @RequestMapping(value = "/membership_renewal_success/employee/email", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testViewMembershipRenewalSuccess_employeeWithEmail(
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        model.addAttribute("isIdentityLoggedIn", false);
        model.addAttribute("patronIsCasEmployee", true);
        model.addAttribute("patronHasEmail", true);
        model.addAttribute("membershipExpiryDate", DateUtils.addDaysToToday(365, "dd/MM/yyyy"));
        model.addAttribute("alephBarcode", "123456789");

        return "membership_renewal_success";
    }

    @RequestMapping(value = "/membership_renewal_success/normal/no-email", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testViewMembershipRenewalSuccess_normalNoEmail(
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        model.addAttribute("isIdentityLoggedIn", false);
        model.addAttribute("patronIsCasEmployee", false);
        model.addAttribute("patronHasEmail", false);
        model.addAttribute("membershipExpiryDate", DateUtils.addDaysToToday(365, "dd/MM/yyyy"));
        model.addAttribute("alephBarcode", "123456789");

        return "membership_renewal_success";
    }

    @RequestMapping(value = "/membership_renewal_success/employee/no-email", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testViewMembershipRenewalSuccess_employeeNoEmail(
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        model.addAttribute("isIdentityLoggedIn", false);
        model.addAttribute("patronIsCasEmployee", true);
        model.addAttribute("patronHasEmail", false);
        model.addAttribute("membershipExpiryDate", DateUtils.addDaysToToday(365, "dd/MM/yyyy"));
        model.addAttribute("alephBarcode", "123456789");

        return "membership_renewal_success";
    }

    /**
     * Converting patron's birthday retrieved from Aleph via the `this.PatronService.getAlephPatron` method in preparation for usage in a Thymeleaf template like so: 
     * <pre>{@code
     * th:value="${T(cz.cas.lib.bankid_registrator.util.DateUtils).convertAlephDate(patron.birthDate)}"
     * }</pre>
     * 
     * <b>EXPLANATION:</b>
     * <p>
     * Normally, Thymeleaf templates containing a new registration form or a membership renewal form use patron data prepared by the `this.PatronService.newPatron` method which converts Bank iD's "yyyy-MM-dd" birthday format into "yyyyMMdd". Such birthday format is then used in the Thymeleaf template like so: 
     * <pre>{@code
     * th:value="${T(cz.cas.lib.bankid_registrator.util.DateUtils).convertAlephDate(patron.birthDate)}"
     * }</pre>
     * </p>
     * <p>However, since all methods in this controller are only for testing Thymeleaf templates, we are using the `this.PatronService.getAlephPatron` method for retrieving Patron data from Aleph instead of using the `this.PatronService.newPatron` for retrieving Patron data from Bank iD. Unlike Bank iD, Aleph returns patron's birthday in the "dd-MM-yyyy".</p>
     * <p>For that reason, this `convertAlephPatronBirthdateForView` method is used to convert patron's birthday into a format which can be used in the Thymeleaf templates as if the patron data were retrieved from Bank iD.</p>
     * 
     * @param alephPatronBirthday
     * @return
     */
    private String convertAlephPatronBirthdateForView(String alephPatronBirthday)
    {
        String testFormat = "dd-MM-yyyy";
        if (DateUtils.isValidDateFormat(alephPatronBirthday, testFormat)) {
            String convertedDate = DateUtils.convertDateFormat(alephPatronBirthday, testFormat, "yyyyMMdd");
            return convertedDate != null ? convertedDate : alephPatronBirthday;
        } else {
            return alephPatronBirthday;
        }
    }
}