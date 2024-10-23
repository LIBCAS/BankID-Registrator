package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.services.AlephService;
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
        PatronService patronService
    ) {
        super(messageSource);
        this.alephService = alephService;
        this.patronService = patronService;

        init();
    }

    @RequestMapping(value = "/callback_registration_new/{patronAlephId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testCallbackRegistrationNew(
        @PathVariable("patronAlephId") String patronAlephId, 
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        Patron bankIdPatron = (Patron) this.alephService.getAlephPatron(patronAlephId, true).get("patron");
        PatronDTO bankIdPatronDTO = this.patronService.getPatronDTO(bankIdPatron);

        model.addAttribute("pageTitle", this.messageSource.getMessage("page.welcome.title", null, locale));
        model.addAttribute("patronId", 1);
        model.addAttribute("patron", bankIdPatronDTO);

        return "callback_registration_new";
    }

    @RequestMapping(value = "/callback_registration_renewal/{patronAlephId}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String testCallbackRegistrationRenewal(
        @PathVariable("patronAlephId") String patronAlephId, 
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        Patron bankIdPatron = (Patron) this.alephService.getAlephPatron(patronAlephId, true).get("patron");
        PatronDTO bankIdPatronDTO = this.patronService.getPatronDTO(bankIdPatron);

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
    public String testCallbackRegistrationRenewalExpiresNow(
        @PathVariable("patronAlephId") String patronAlephId, 
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        Patron bankIdPatron = (Patron) this.alephService.getAlephPatron(patronAlephId, true).get("patron");
        PatronDTO bankIdPatronDTO = this.patronService.getPatronDTO(bankIdPatron);

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
}