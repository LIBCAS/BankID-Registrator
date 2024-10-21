package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.PatronService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.util.Locale;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/test")
public class TestMainController
{
    private final MessageSource messageSource;
    private final AlephService alephService;
    private final PatronService patronService;

    @Autowired
    public TestMainController(MessageSource messageSource, AlephService alephService, PatronService patronService)
    {
        this.messageSource = messageSource;
        this.alephService = alephService;
        this.patronService = patronService;
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
}