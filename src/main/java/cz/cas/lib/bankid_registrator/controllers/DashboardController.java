package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.services.IdentityService;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for the admin dashboard page
 */
@Controller
public class DashboardController extends AdminControllerAbstract
{
    private static final int PAGE_SIZE = 100;

    private final IdentityService identityService;

    @NotEmpty
    @Value("${spring.application.name} - Admin Dashboard")
    private String appName;

    public DashboardController(MessageSource messageSource, IdentityService identityService) {
        super(messageSource);
        this.identityService = identityService;
    }

    /**
     * Admin dashboard - identity list
     * @param model
     * @param locale
     * @param page
     * @param sortField
     * @param sortDir
     * @param searchAlephId
     * @param searchAlephBarcode
     * @param filterCasEmployee
     * @param filterCheckedByAdmin
     * @return 
     */
    @RequestMapping(value = "/dashboard", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getDashboard(
        Model model, 
        Locale locale, 
        @RequestParam(defaultValue = "0") int page, 
        @RequestParam(defaultValue = "id") String sortField, 
        @RequestParam(defaultValue = "asc") String sortDir, 
        @RequestParam(defaultValue = "") String searchAlephId, 
        @RequestParam(defaultValue = "") String searchAlephBarcode, 
        @RequestParam(required = false) Boolean filterCasEmployee, 
        @RequestParam(required = false) Boolean filterCheckedByAdmin
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        PageRequest pageable = PageRequest.of(page, DashboardController.PAGE_SIZE, sort);
        Page<Identity> identityPage = identityService.findIdentities(pageable, searchAlephId, searchAlephBarcode, filterCasEmployee, filterCheckedByAdmin);

        model.addAttribute("identityPage", identityPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", identityPage.getTotalPages());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("searchAlephId", searchAlephId);
        model.addAttribute("searchAlephBarcode", searchAlephBarcode);
        model.addAttribute("filterCasEmployee", filterCasEmployee);
        model.addAttribute("filterCheckedByAdmin", filterCheckedByAdmin);

        model.addAttribute("dateUtils", new DateUtils());

        return "dashboard";
    }

    /**
     * Admin dashboard - identity detail / Aleph Patron detail
     */
    @RequestMapping(value = "/dashboard/identity", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getIdentityDetail(
        Model model, 
        Locale locale, 
        @RequestParam String id
    ) {
        Optional<Identity> identity = identityService.findById(Long.parseLong(id));

        if (!identity.isPresent()) {
            return "redirect:/dashboard";
        }

        model.addAttribute("identity", identity);
        return "identity_detail";
    }
}
