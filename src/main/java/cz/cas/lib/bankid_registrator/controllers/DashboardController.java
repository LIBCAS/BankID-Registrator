    package cz.cas.lib.bankid_registrator.controllers;

    import cz.cas.lib.bankid_registrator.model.identity.Identity;
    import cz.cas.lib.bankid_registrator.model.identity.IdentityActivity;
    import cz.cas.lib.bankid_registrator.model.media.Media;
    import cz.cas.lib.bankid_registrator.model.patron.Patron;
    import cz.cas.lib.bankid_registrator.services.AlephService;
    import cz.cas.lib.bankid_registrator.services.IdentityActivityService;
    import cz.cas.lib.bankid_registrator.services.IdentityService;
    import cz.cas.lib.bankid_registrator.services.MediaService;
    import cz.cas.lib.bankid_registrator.util.DateUtils;
    import java.util.List;
    import java.util.Locale;
    import java.util.Map;
    import java.util.Optional;
    import org.springframework.context.MessageSource;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Sort;
    import org.springframework.http.MediaType;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.PathVariable;
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

        private final AlephService alephService;
        private final IdentityActivityService identityActivityService;
        private final IdentityService identityService;
        private final MediaService mediaService;

        public DashboardController(
            MessageSource messageSource, 
            AlephService alephService, 
            IdentityActivityService identityActivityService, 
            IdentityService identityService, 
            MediaService mediaService
        ) {
            super(messageSource);
            this.alephService = alephService;
            this.identityActivityService = identityActivityService;
            this.identityService = identityService;
            this.mediaService = mediaService;
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
         * @param filterSoftDeleted
         * @return 
         */
        @RequestMapping(value = "/dashboard", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
        public String viewDashboard(
            Model model, 
            Locale locale, 
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "id") String sortField, 
            @RequestParam(defaultValue = "asc") String sortDir, 
            @RequestParam(required = false) String searchAlephIdOrBarcode, 
            @RequestParam(required = false) Boolean filterCasEmployee, 
            @RequestParam(required = false) Boolean filterCheckedByAdmin,
            @RequestParam(defaultValue = "true") Boolean filterSoftDeleted
        ) {
            // this.logger.info("Searching for identities with Aleph ID / barcode: " + searchAlephIdOrBarcode + ", CAS employee: " + filterCasEmployee + ", checked by admin: " + filterCheckedByAdmin + ", exclude soft-deleted: " + filterSoftDeleted);
            Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
            PageRequest pageable = PageRequest.of(page, DashboardController.PAGE_SIZE, sort);
            Page<Identity> identityPage = this.identityService.findIdentities(pageable, searchAlephIdOrBarcode, filterCasEmployee, filterCheckedByAdmin, filterSoftDeleted);

            model.addAttribute("pageTitle", this.messageSource.getMessage("page.identitiesDashboard.title", null, locale));

            model.addAttribute("identityPage", identityPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", identityPage.getTotalPages());
            model.addAttribute("sortField", sortField);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
            model.addAttribute("searchAlephIdOrBarcode", searchAlephIdOrBarcode);
            model.addAttribute("filterCasEmployee", filterCasEmployee);
            model.addAttribute("filterCheckedByAdmin", filterCheckedByAdmin);
            model.addAttribute("filterSoftDeleted", filterSoftDeleted);

            return "dashboard";
        }

        /**
         * Admin dashboard - identity detail / Aleph Patron detail
         */
        @RequestMapping(value = "/dashboard/identity/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
        public String viewIdentityDetail(
            Model model, 
            Locale locale, 
            @PathVariable("id") Long identityId
        ) {
            Optional<Identity> identitySearch = this.identityService.findById(identityId);

            if (identitySearch.isPresent()) {
                Identity identity = identitySearch.get();
                List<Media> medias = this.mediaService.findByIdentityId(identityId);
                List<IdentityActivity> activities = this.identityActivityService.findByIdentityId(identityId);

                if (identity.getAlephId() != null) {
                    Map<String, Object> alephPatronGet = alephService.getAlephPatron(identity.getAlephId(), true);

                    if (alephPatronGet.containsKey("error")) {
                        throw new RuntimeException("Error while retrieving Aleph patron: " + alephPatronGet.get("error"));
                    }

                    Patron alephPatron = (Patron) alephPatronGet.get("patron");
                    String alephPatronExpiryDate = alephPatron.getExpiryDate();

                    model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityDetail.title", null, locale) + " " + alephPatron.getBarcode());
                    model.addAttribute("patron", alephPatron);
                    model.addAttribute("membershipExpiryDate", alephPatronExpiryDate);
                    model.addAttribute("membershipHasExpired", DateUtils.isDateExpired(alephPatronExpiryDate, "dd/MM/yyyy"));
                    model.addAttribute("membershipExpiresToday", DateUtils.isDateToday(alephPatronExpiryDate, "dd/MM/yyyy"));
                } else {
                    model.addAttribute("pageTitle", this.messageSource.getMessage("message.identity", null, locale));
                }

                model.addAttribute("identity", identity);
                model.addAttribute("medias", medias);
                model.addAttribute("activities", activities);

                return "identity_detail";
            } else {
                return "redirect:/dashboard";
            }
        }

        /**
         * Toggles the checked-by-admin status of an identity and redirects to the identity detail view.
         */
        @RequestMapping(value = "/dashboard/identity/{id}/toggle-checked", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
        public String toggleCheckedByAdmin(
            @PathVariable("id") Long identityId
        ) {
            Optional<Identity> identitySearch = identityService.findById(identityId);

            if (identitySearch.isPresent()) {
                Identity identity = identitySearch.get();
                identity.setCheckedByAdmin(!identity.getCheckedByAdmin() ? true : !identity.getCheckedByAdmin());
                identityService.save(identity);
            }

            return "redirect:/dashboard/identity/" + identityId;
        }

        /**
         * Deletes an identity and redirects to the dashboard.
         */
        @RequestMapping(value = "/dashboard/identity/{id}/delete", method = RequestMethod.GET)
        public String deleteIdentity(Locale locale, @PathVariable("id") Long identityId) {
            Optional<Identity> identityOpt = identityService.findById(identityId);

            if (identityOpt.isPresent()) {
                Identity identity = identityOpt.get();

                // Soft-delete identity
                identity.setDeleted(true);
                identityService.save(identity);

                // Delete media files
                List<Media> medias = mediaService.findByIdentityId(identityId);
                for (Media media : medias) {
                    try {
                        mediaService.delete(media);
                    } catch (RuntimeException e) {
                        throw new RuntimeException(this.messageSource.getMessage("error.media.failedToDelete", null, locale) + " " + media.getName() + ": " + e.getMessage(), e);
                    }
                }

                this.identityActivityService.logIdentityDeleted(identity);
            }

            return "redirect:/dashboard";
        }

        /**
         * Restores a soft-deleted identity and redirects to the dashboard.
         */
        @RequestMapping(value = "/dashboard/identity/{id}/restore", method = RequestMethod.GET)
        public String restoreIdentity(Locale locale, @PathVariable("id") Long identityId) {
            Optional<Identity> identityOpt = identityService.findById(identityId);

            if (identityOpt.isPresent()) {
                Identity identity = identityOpt.get();

                // Restore a soft-deleted identity
                identity.setDeleted(false);
                identityService.save(identity);

                this.identityActivityService.logIdentityRestored(identity);
            }

            return "redirect:/dashboard";
        }

        /**
         * Marks an identity as deleted in Aleph; this will unpair patron barcode and patron ID from the identity 
         */
        @RequestMapping(value = "/dashboard/identity/{id}/aleph-deleted", method = RequestMethod.GET)
        public String markIdentityAsDeletedInAleph(Locale locale, @PathVariable("id") Long identityId) {
            Optional<Identity> identityOpt = identityService.findById(identityId);

            if (identityOpt.isPresent()) {
                Identity identity = identityOpt.get();

                identity.setAlephDeleted(true);
                identity.setAlephBarcode(null);
                identity.setAlephId(null);
                identity.setDeleted(true);
                identityService.save(identity);

                // Delete media files
                List<Media> medias = mediaService.findByIdentityId(identityId);
                for (Media media : medias) {
                    try {
                        mediaService.delete(media);
                    } catch (RuntimeException e) {
                        throw new RuntimeException(this.messageSource.getMessage("error.media.failedToDelete", null, locale) + " " + media.getName() + ": " + e.getMessage(), e);
                    }
                }

                this.identityActivityService.logIdentityDeleted(identity);
                this.identityActivityService.logIdentityMarkedAsDeletedInAleph(identity);
            }

            return "redirect:/dashboard";
        }
    }
