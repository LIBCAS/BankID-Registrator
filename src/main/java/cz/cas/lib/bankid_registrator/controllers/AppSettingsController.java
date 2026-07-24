package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.model.app_settings.SupportEmail;
import cz.cas.lib.bankid_registrator.services.AppSettingsService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AppSettingsController extends AdminControllerAbstract
{
    private static final int MIN_VISIBLE_ROWS = 5;
    private final AppSettingsService appSettingsService;

    public AppSettingsController(MessageSource messageSource, AppSettingsService appSettingsService) {
        super(messageSource);
        this.appSettingsService = appSettingsService;
    }

    @RequestMapping(value = "/dashboard/settings", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String viewSettings(Model model, Locale locale) {
        List<SupportEmail> supportEmails = this.appSettingsService.getSupportEmails();
        int rowsToRender = Math.max(MIN_VISIBLE_ROWS, supportEmails.size() + 1);
        List<SupportEmail> supportEmailRows = new ArrayList<>(supportEmails);
        while (supportEmailRows.size() < rowsToRender) {
            supportEmailRows.add(new SupportEmail());
        }
        model.addAttribute("pageTitle", this.messageSource.getMessage("appSettings.title", null, locale));
        model.addAttribute("supportEmailRows", supportEmailRows);
        model.addAttribute("voucherPrinterAppUrl", this.appSettingsService.getVoucherPrinterAppUrl().orElse(""));
        return "app_settings";
    }

    @RequestMapping(value = "/dashboard/settings/support-emails", method = RequestMethod.POST)
    public String updateSupportEmails(
        @RequestParam(required = false) List<String> emails,
        @RequestParam(required = false) Integer primaryIndex,
        RedirectAttributes redirectAttributes,
        Locale locale
    ) {
        try {
            this.appSettingsService.replaceSupportEmails(emails, primaryIndex);
            redirectAttributes.addFlashAttribute("successMessage",
                this.messageSource.getMessage("appSettings.supportEmails.updated", null, locale));
        } catch (IllegalArgumentException e) {
            getLogger().warn("Failed to update support emails: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                this.messageSource.getMessage("appSettings.supportEmails.updateFailed", null, locale));
        }
        return "redirect:/dashboard/settings";
    }

    @RequestMapping(value = "/dashboard/settings/voucher-printer", method = RequestMethod.POST)
    public String updateVoucherPrinterAppUrl(
        @RequestParam(required = false) String voucherPrinterAppUrl,
        RedirectAttributes redirectAttributes,
        Locale locale
    ) {
        try {
            this.appSettingsService.updateVoucherPrinterAppUrl(voucherPrinterAppUrl);
            redirectAttributes.addFlashAttribute("successMessage",
                this.messageSource.getMessage("appSettings.voucherPrinter.updated", null, locale));
        } catch (IllegalArgumentException e) {
            getLogger().warn("Failed to update Voucher printer app URL: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage",
                this.messageSource.getMessage("appSettings.voucherPrinter.updateFailed", null, locale));
        }
        return "redirect:/dashboard/settings";
    }
}
