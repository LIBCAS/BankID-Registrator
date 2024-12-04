package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.model.user.User;
import cz.cas.lib.bankid_registrator.services.AppUserDetailsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController
{
    @NotEmpty
    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private AppUserDetailsService userDetailsService;

    // @GetMapping("/user/register")
    // public String showRegistrationForm(Model model)
    // {
    //     model.addAttribute("user", new User());
    //     return "admin_registration";
    // }

    // @PostMapping("/user/register")
    // public String registerUser(User user)
    // {
    //     userDetailsService.saveUser(user);
    //     return "redirect:/user/login";
    // }

    @GetMapping("/user/login")
    public String showLoginForm(Authentication authentication)
    {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "admin_login";
    }

    @GetMapping("/user/logout")
    public String logoutPage (HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null){    
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/user/login";
    }
}