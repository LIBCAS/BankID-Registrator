/*
 * Copyright (C) 2022 Academy of Sciences Library
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.dao.mariadb.MariaDBRepository;
import cz.cas.lib.bankid_registrator.model.patron_barcode.PatronBarcode;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.MainService;
import cz.cas.lib.bankid_registrator.valueobjs.AccessTokenContainer;
import java.net.URI;
import javax.servlet.ServletContext;
import javax.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author iok
 */
@Controller
public class MainController extends MainControllerAbstract {

    @Autowired
    private MainConfiguration mainConfig;

    @Autowired
    private MainService mainService;

    @Autowired
    private AlephService alephService;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private ApplicationContext applicationContext;

    // jpa test
    @Autowired
    private MariaDBRepository mariaDBRepository;
    //

    @Autowired
    private AccessTokenContainer accessTokenContainer;

    @NotEmpty
    @Value("${spring.application.name}")
    private String appName;

    public MainController() {
        super();
        init();
    }

    @Override
    @RequestMapping(value="/", method=RequestMethod.GET)
    public String RootEntry() {
        return "redirect:/welcome";
    }

    @Override
    @RequestMapping(value="/index", method=RequestMethod.GET)
    public String IndexEntry() {
        return "redirect:/welcome";
    }

    /**
     * 
     * @param model
     * @return 
     */
    @RequestMapping(value="/welcome", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String WelcomeEntry(Model model) {

        model.addAttribute("appName", this.appName);
        model.addAttribute("loginEndpoint", this.servletContext.getContextPath().concat("/login"));

        return "welcome";
    }

    /**
     * 
     * @return 
     */
    @RequestMapping(value="/login", method=RequestMethod.GET)
    public String InitiateLoginEntry() {
        getLogger().info("ACCESSING LOGIN PAGE ...");
        StringBuilder strTmp = new StringBuilder(0);

        URI authorizationEndpoint = this.mainService.getBankIDAuthorizationEndpoint(this.mainConfig.getIssuer_url());
        getLogger().info("authorizationEndpoint: {}", authorizationEndpoint);
        if (authorizationEndpoint == null) {
            // redirect to error page
            return "error";
        }

        strTmp.append(authorizationEndpoint.toString().concat("?"));

        String loginURL = this.mainService.getBankIDLoginURL(authorizationEndpoint.toString());
        getLogger().info("loginURL: {}", loginURL);
        if (loginURL == null) {
            // redirect to error page
            return "error";
        }

        strTmp.append(loginURL);

        return "redirect:".concat(strTmp.toString());
    }

    /**
     * 
     * @param code
     * @param model
     * @return 
     */
    @RequestMapping(value="/callback", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String CallbackEntry(@RequestParam("code") String code, Model model) {

        model.addAttribute("appName", this.appName);

        Assert.notNull(code, "\"code\" is required");

        if (!accessTokenContainer.getCodeTokenMap().containsKey(code)) {
            accessTokenContainer.setAccessToken(code, this.mainService.getTokenExchange(code).getAccessToken());
        } else {
            // TODO valid access token
        }
        
        Connect userInfo = this.mainService.getUserInfo(accessTokenContainer.getAccessToken(code));

        Identify userProfile = this.mainService.getProfile(accessTokenContainer.getAccessToken(code));

        if (userProfile.getLimited_legal_capacity()) {
            // TODO limited_legal_capacity => go out
            model.addAttribute("errorMessage", "Registrace byla zamítnuta: Osoba s omezenou způsobilostí k právním úkonům.");
            return "error";
        }

        model.addAttribute("userInfo", userInfo);

        model.addAttribute("userProfile", userProfile);

        model.addAttribute("patronXML", this.alephService.CreatePatronXML(userInfo, userProfile));

        model.addAttribute("bankIDForSep", "https://developer.bankid.cz/docs/api/bankid-for-sep");

        // jpa test - todo lock/synchronized
        PatronBarcode patronBarcode = new PatronBarcode();
        patronBarcode.setSub(userProfile.getSub());
        patronBarcode.setBarcode(this.mainConfig.getBarcode_prefix().concat(code));
        this.mariaDBRepository.save(patronBarcode);
        //

        return "callback";

    }

    /**
     * 
     * @param model
     * @return 
     */
    @RequestMapping(value="/tos", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String TermsOfServiceEntry(Model model) {

        model.addAttribute("appName", this.appName);

        return "terms_of_service";
    }

    /**
     * 
     * @param model
     * @return 
     */
    @RequestMapping(value="/privacy-policy", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String UsagePolicyEntry(Model model) {

        model.addAttribute("appName", this.appName);

        return "data_usage_policy";
    }

}
