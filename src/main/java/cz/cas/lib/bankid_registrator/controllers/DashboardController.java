package cz.cas.lib.bankid_registrator.controllers;

import javax.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import cz.cas.lib.bankid_registrator.services.PatronService;

/**
 * Controller for the admin dashboard page
 */
@Controller
public class DashboardController extends ControllerAbstract {
    private final PatronService patronService;

    @NotEmpty
    @Value("${spring.application.name} - Admin Dashboard")
    private String appName;

    public DashboardController(PatronService patronService) {
        this.patronService = patronService;
    }

    /**
     * Admin dashboard
     * @param model
     * @return 
     */
    @RequestMapping(value="/dashboard", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String DashboardEntry(Model model) {
        getLogger().info("ACCESSING DASHBOARD PAGE ...");
        model.addAttribute("appName", this.appName);

        model.addAttribute("data", this.patronService.findCasEmployeesWithMedia());

        return "dashboard";
    }
}
