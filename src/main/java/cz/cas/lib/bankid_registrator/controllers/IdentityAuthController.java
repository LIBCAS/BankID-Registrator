package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.services.IdentityAuthService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handle identity (Bank id related) authentication
 */
@Controller
public class IdentityAuthController
{
    @Autowired
    private IdentityAuthService identityAuthService;

    /**
     * Route for logging out the Bank iD verified identity with redirect
     * @param redirect Redirect URL
     * @param request
     */
    @GetMapping("/identity/logout-redirect")
    public String routeLogout(
        @RequestParam(value = "redirect", required = false) String redirect, 
        HttpServletRequest request
    ) {
        this.identityAuthService.logout(request);

        return "redirect:" + (redirect != null ? redirect : "/welcome");
    }
}
