    package cz.cas.lib.bankid_registrator.controllers;

    import cz.cas.lib.bankid_registrator.entities.media.MediaSubmissionType;
    import cz.cas.lib.bankid_registrator.entities.patron.PatronFineStatus;
    import cz.cas.lib.bankid_registrator.model.identity.Identity;
    import cz.cas.lib.bankid_registrator.model.identity.IdentityActivity;
    import cz.cas.lib.bankid_registrator.model.media.Media;
    import cz.cas.lib.bankid_registrator.model.patron.Patron;
    import cz.cas.lib.bankid_registrator.model.voucher.VoucherUsage;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import cz.cas.lib.bankid_registrator.services.AlephService;
    import cz.cas.lib.bankid_registrator.services.IdentityActivityService;
    import cz.cas.lib.bankid_registrator.services.IdentityService;
    import cz.cas.lib.bankid_registrator.services.MediaService;
    import cz.cas.lib.bankid_registrator.services.VoucherService;
    import cz.cas.lib.bankid_registrator.util.DateUtils;
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.Collections;
    import java.util.HashSet;
    import java.util.LinkedHashMap;
    import java.util.List;
    import java.util.Locale;
    import java.util.Map;
    import java.util.Objects;
    import java.util.Optional;
    import java.util.Set;
    import java.util.stream.Collectors;
    import org.springframework.context.MessageSource;
    // import org.springframework.util.StopWatch;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageImpl;
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
        private static final int PAGE_SIZE = 50;

        private final AlephService alephService;
        private final IdentityActivityService identityActivityService;
        private final IdentityService identityService;
        private final MediaService mediaService;
        private final VoucherService voucherService;

        public DashboardController(
            MessageSource messageSource, 
            AlephService alephService, 
            IdentityActivityService identityActivityService, 
            IdentityService identityService, 
            MediaService mediaService,
            VoucherService voucherService
        ) {
            super(messageSource);
            this.alephService = alephService;
            this.identityActivityService = identityActivityService;
            this.identityService = identityService;
            this.mediaService = mediaService;
            this.voucherService = voucherService;
        }

        /**
         * Admin dashboard - identity list
         * @param model
         * @param locale
         * @param page
         * @param sortField
         * @param sortDir
         * @param searchAlephIdOrBarcode
         * @param searchFullname
         * @param filterCasEmployee
         * @param filterCheckedByAdmin
         * @param filterPasswordSet
         * @param filterPaymentStatus
         * @param filterSoftDeleted
         * @return 
         */
        @RequestMapping(value = "/dashboard", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
        public String viewDashboard(
            Model model, 
            Locale locale, 
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "updatedAt") String sortField, 
            @RequestParam(defaultValue = "asc") String sortDir, 
            @RequestParam(required = false) String searchAlephIdOrBarcode, 
            @RequestParam(required = false) String searchFullname, 
            @RequestParam(required = false) Boolean filterCasEmployee, 
            @RequestParam(required = false) Boolean filterCheckedByAdmin,
            @RequestParam(required = false) Boolean filterPasswordSet,
            @RequestParam(required = false) List<String> filterPaymentStatus,
            @RequestParam(defaultValue = "true") Boolean filterSoftDeleted
        ) {
            // StopWatch stopWatch = new StopWatch("Dashboard Performance");

            // stopWatch.start("MySQL Query");
            Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();

            List<Identity> allMatchingIdentities = this.identityService.findAllIdentities(searchAlephIdOrBarcode, filterCasEmployee, filterCheckedByAdmin, filterPasswordSet, filterSoftDeleted, sort);
            // stopWatch.stop();

            // logger.info("MySQL query returned {} records", allMatchingIdentities.size());

            // stopWatch.start("Extract Aleph IDs");
            List<String> patronIds = allMatchingIdentities.stream()
                .map(Identity::getAlephId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            // stopWatch.stop();

            // stopWatch.start("Oracle Query");
            Map<String, List<Object[]>> identitiesAlephData = this.alephService.getBulkPatronsData(patronIds);
            // stopWatch.stop();

            // logger.info("Oracle query returned data for {} patrons", identitiesAlephData.size());

            // stopWatch.start("Apply Oracle Filters");
            Set<String> matchingAlephIds = new HashSet<>();

            if ((searchFullname != null && !searchFullname.isEmpty()) || 
                (filterPaymentStatus != null && !filterPaymentStatus.isEmpty())) {
                
                identitiesAlephData.forEach((alephId, patronRecords) -> {
                    // Filter by payment status
                    boolean matchesPaymentStatus = filterPaymentStatus != null && !filterPaymentStatus.isEmpty() 
                        ? patronRecords.stream().anyMatch(record -> filterPaymentStatus.contains(String.valueOf(record[1])))
                        : true;

                    // Filter by full name
                    boolean matchesFullname = searchFullname != null && !searchFullname.isEmpty()
                        ? patronRecords.stream().anyMatch(record -> {
                            String fullname = String.valueOf(record[2]).toLowerCase();
                            return fullname.contains(searchFullname.toLowerCase());
                        })
                        : true;

                    if (matchesPaymentStatus && matchesFullname) {
                        matchingAlephIds.add(alephId);
                    }
                });
            } else {
                // If no Oracle filters, all IDs match
                matchingAlephIds.addAll(patronIds);
            }
            // stopWatch.stop();

            // stopWatch.start("Create Intersection");
            List<Identity> finalFilteredList = allMatchingIdentities.stream()
                .filter(identity -> {
                    String alephId = identity.getAlephId();
                    return alephId == null || matchingAlephIds.contains(alephId);
                })
                .collect(Collectors.toList());
            // stopWatch.stop();

            // logger.info("After all filtering, {} records remain", finalFilteredList.size());

            // stopWatch.start("Apply Pagination");
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, finalFilteredList.size());
            // stopWatch.stop();

            // Handle case where page is beyond available data
            if (start >= finalFilteredList.size() && !finalFilteredList.isEmpty()) {
                page = (finalFilteredList.size() - 1) / PAGE_SIZE;
                start = page * PAGE_SIZE;
                end = Math.min(start + PAGE_SIZE, finalFilteredList.size());
            }

            List<Identity> pageContent = start < end ? 
                finalFilteredList.subList(start, end) : 
                Collections.emptyList();

            Page<Identity> finalPage = new PageImpl<>(
                pageContent, 
                PageRequest.of(page, PAGE_SIZE, sort), 
                finalFilteredList.size());

            // Translate payment statuses
            identitiesAlephData.forEach((patronId, rows) -> {
                for (Object[] row : rows) {
                    String translatedStatus = messageSource.getMessage(
                        "patronPaymentStatus." + String.valueOf(row[1]),
                        null,
                        locale
                    );
                    Object[] updatedRow = Arrays.copyOf(row, row.length + 1);
                    updatedRow[row.length] = translatedStatus;
                    rows.set(rows.indexOf(row), updatedRow);
                }
            });

            model.addAttribute("pageTitle", this.messageSource.getMessage("page.identitiesDashboard.title", null, locale));
            model.addAttribute("identityPage", finalPage);
            model.addAttribute("identitiesAlephData", identitiesAlephData);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", finalPage.getTotalPages());
            model.addAttribute("sortField", sortField);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
            model.addAttribute("searchAlephIdOrBarcode", searchAlephIdOrBarcode);
            model.addAttribute("searchFullname", searchFullname);
            model.addAttribute("filterCasEmployee", filterCasEmployee);
            model.addAttribute("filterCheckedByAdmin", filterCheckedByAdmin);
            model.addAttribute("filterPasswordSet", filterPasswordSet);
            model.addAttribute("filterSoftDeleted", filterSoftDeleted);
            model.addAttribute("filterPaymentStatus", filterPaymentStatus);
            model.addAttribute("patronFineStatusMap", Map.of(
                PatronFineStatus.PAID.getKey(), "bg-green-100 text-green-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm",
                PatronFineStatus.NOT_PAID.getKey(), "bg-red-100 text-red-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm",
                PatronFineStatus.CANCELLED.getKey(), "bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm",
                PatronFineStatus.FREE_OR_UNKNOWN.getKey(), "bg-yellow-100 text-yellow-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm"
            ));

            // logger.info(stopWatch.prettyPrint());

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
                List<VoucherUsage> voucherUsages = this.voucherService.getUsagesByIdentity(identity);
                List<VoucherUsage> confirmedVoucherUsages = new ArrayList<>();
                List<VoucherUsage> pendingVoucherUsages = new ArrayList<>();

                for (VoucherUsage usage : voucherUsages) {
                    if (usage.isConfirmed()) {
                        confirmedVoucherUsages.add(usage);
                    } else {
                        pendingVoucherUsages.add(usage);
                    }
                }

                String paymentStatusKey = PatronFineStatus.FREE_OR_UNKNOWN.getKey();
                String paymentStatusClass = getPaymentStatusClass(paymentStatusKey);

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

                    Map<String, List<Object[]>> alephPaymentData = this.alephService.getBulkPatronsData(Collections.singletonList(identity.getAlephId()));
                    List<Object[]> patronPaymentRows = alephPaymentData.get(identity.getAlephId());
                    if (patronPaymentRows != null && !patronPaymentRows.isEmpty()) {
                        paymentStatusKey = String.valueOf(patronPaymentRows.get(0)[1]);
                        paymentStatusClass = getPaymentStatusClass(paymentStatusKey);
                    }
                } else {
                    model.addAttribute("pageTitle", this.messageSource.getMessage("message.identity", null, locale));
                }

                model.addAttribute("identity", identity);
                model.addAttribute("medias", medias);
                List<MediaGroupView> mediaGroups = buildMediaGroups(medias, locale);
                model.addAttribute("currentMediaGroup", mediaGroups.isEmpty() ? null : mediaGroups.get(0));
                model.addAttribute("historicalMediaGroups", mediaGroups.size() > 1 ? mediaGroups.subList(1, mediaGroups.size()) : Collections.emptyList());
                model.addAttribute("activities", activities);
                model.addAttribute("paymentStatusKey", paymentStatusKey);
                model.addAttribute("paymentStatusClass", paymentStatusClass);
                model.addAttribute("confirmedVoucherUsages", confirmedVoucherUsages);
                model.addAttribute("pendingVoucherUsages", pendingVoucherUsages);

                return "identity_detail";
            } else {
                return "redirect:/dashboard";
            }
        }

        private String getPaymentStatusClass(String paymentStatusKey) {
            return Map.of(
                PatronFineStatus.PAID.getKey(), "bg-green-100 text-green-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm",
                PatronFineStatus.NOT_PAID.getKey(), "bg-red-100 text-red-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm",
                PatronFineStatus.CANCELLED.getKey(), "bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm",
                PatronFineStatus.FREE_OR_UNKNOWN.getKey(), "bg-yellow-100 text-yellow-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm"
            ).getOrDefault(paymentStatusKey, "bg-yellow-100 text-yellow-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm");
        }

        private List<MediaGroupView> buildMediaGroups(List<Media> mediaList, Locale locale) {
            if (mediaList == null || mediaList.isEmpty()) {
                return Collections.emptyList();
            }

            List<Media> legacyMedia = mediaList.stream()
                .filter(media -> media.getSubmissionBatchId() == null || media.getSubmissionBatchId().trim().isEmpty())
                .collect(Collectors.toList());

            Map<String, List<Media>> groupedMedia = mediaList.stream()
                .filter(media -> media.getSubmissionBatchId() != null && !media.getSubmissionBatchId().trim().isEmpty())
                .collect(Collectors.groupingBy(Media::getSubmissionBatchId, LinkedHashMap::new, Collectors.toList()));

            List<MediaGroupView> groups = new ArrayList<>();
            if (!legacyMedia.isEmpty()) {
                groups.add(new MediaGroupView(
                    "legacy",
                    this.messageSource.getMessage("message.attachmentsLegacy", null, locale),
                    null,
                    null,
                    legacyMedia
                ));
            }

            for (Map.Entry<String, List<Media>> entry : groupedMedia.entrySet()) {
                List<Media> groupedItems = new ArrayList<>(entry.getValue());
                groupedItems.sort(
                    java.util.Comparator.comparing(Media::getBatchOrderIndex, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(Media::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(Media::getId, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                );

                Media firstMedia = groupedItems.get(0);
                groups.add(new MediaGroupView(
                    entry.getKey(),
                    getMediaGroupTitle(firstMedia.getSubmissionType(), locale),
                    firstMedia.getCreatedAt(),
                    formatMediaGroupSubmittedAt(firstMedia.getCreatedAt()),
                    groupedItems
                ));
            }

            groups.sort(
                java.util.Comparator.comparing(MediaGroupView::getSubmittedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                    .thenComparing(MediaGroupView::getBatchId, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
            );

            return groups;
        }

        private String getMediaGroupTitle(MediaSubmissionType submissionType, Locale locale) {
            if (submissionType == MediaSubmissionType.REGISTRATION) {
                return this.messageSource.getMessage("message.attachmentsRegistration", null, locale);
            }

            if (submissionType == MediaSubmissionType.RENEWAL) {
                return this.messageSource.getMessage("message.attachmentsRenewal", null, locale);
            }

            return this.messageSource.getMessage("message.attachments", null, locale);
        }

        private String formatMediaGroupSubmittedAt(LocalDateTime submittedAt) {
            if (submittedAt == null) {
                return null;
            }

            return submittedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
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
                        throw new RuntimeException(this.messageSource.getMessage("error.media.failedToDelete", null, locale) + " " + media.getDisplayName() + ": " + e.getMessage(), e);
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
                        throw new RuntimeException(this.messageSource.getMessage("error.media.failedToDelete", null, locale) + " " + media.getDisplayName() + ": " + e.getMessage(), e);
                    }
                }

                this.identityActivityService.logIdentityDeleted(identity);
                this.identityActivityService.logIdentityMarkedAsDeletedInAleph(identity);
            }

            return "redirect:/dashboard";
        }

        public static class MediaGroupView
        {
            private final String batchId;
            private final String title;
            private final LocalDateTime submittedAt;
            private final String submittedAtLabel;
            private final List<Media> medias;

            public MediaGroupView(String batchId, String title, LocalDateTime submittedAt, String submittedAtLabel, List<Media> medias) {
                this.batchId = batchId;
                this.title = title;
                this.submittedAt = submittedAt;
                this.submittedAtLabel = submittedAtLabel;
                this.medias = medias;
            }

            public String getBatchId() {
                return batchId;
            }

            public String getTitle() {
                return title;
            }

            public LocalDateTime getSubmittedAt() {
                return submittedAt;
            }

            public String getSubmittedAtLabel() {
                return submittedAtLabel;
            }

            public List<Media> getMedias() {
                return medias;
            }
        }
    }
