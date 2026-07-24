package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.RegistrationFeeConfig;
import cz.cas.lib.bankid_registrator.configurations.VoucherIssuanceConfig;
import cz.cas.lib.bankid_registrator.dto.VoucherPrintData;
import cz.cas.lib.bankid_registrator.entities.voucher.DiscountType;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherRecipientType;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherStatus;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherType;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import cz.cas.lib.bankid_registrator.model.voucher.VoucherUsage;
import cz.cas.lib.bankid_registrator.services.AppSettingsService;
import cz.cas.lib.bankid_registrator.services.VoucherPrinterService;
import cz.cas.lib.bankid_registrator.services.VoucherService;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin controller for voucher management
 */
@Controller
public class VoucherController extends AdminControllerAbstract
{
    private static final int PAGE_SIZE = 25;
    private static final int MAX_BATCH_PRINT_SIZE = 500;

    private final VoucherService voucherService;
    private final AppSettingsService appSettingsService;
    private final VoucherPrinterService voucherPrinterService;
    private final RegistrationFeeConfig registrationFeeConfig;
    private final VoucherIssuanceConfig voucherIssuanceConfig;

    public VoucherController(
        MessageSource messageSource,
        VoucherService voucherService,
        AppSettingsService appSettingsService,
        VoucherPrinterService voucherPrinterService,
        RegistrationFeeConfig registrationFeeConfig,
        VoucherIssuanceConfig voucherIssuanceConfig
    ) {
        super(messageSource);
        this.voucherService = voucherService;
        this.appSettingsService = appSettingsService;
        this.voucherPrinterService = voucherPrinterService;
        this.registrationFeeConfig = registrationFeeConfig;
        this.voucherIssuanceConfig = voucherIssuanceConfig;
    }

    /**
     * Voucher list page
     */
    @RequestMapping(value = "/dashboard/vouchers", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String listVouchers(
        Model model,
        Locale locale,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) String voucherSearch,
        @RequestParam(required = false) String patronId,
        @RequestParam(required = false) List<VoucherStatus> statuses,
        @RequestParam(required = false) VoucherType type,
        @RequestParam(required = false) DiscountType discountType,
        @RequestParam(required = false) String expiresFrom,
        @RequestParam(required = false) String expiresTo,
        @RequestParam(required = false) String firstUsedFrom,
        @RequestParam(required = false) String firstUsedTo,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Auto-expire vouchers past their expiration date
        this.voucherService.expireActiveVouchers();

        // When no statuses are selected or all are selected, pass all values
        List<VoucherStatus> effectiveStatuses = (statuses == null || statuses.isEmpty() ||
            statuses.size() == VoucherStatus.values().length) ? Arrays.asList(VoucherStatus.values()) : statuses;
        boolean statusFilterActive = statuses != null && !statuses.isEmpty() && statuses.size() != VoucherStatus.values().length;
        boolean activeFilters = hasText(voucherSearch) || hasText(patronId) || statusFilterActive || type != null || discountType != null ||
            hasText(expiresFrom) || hasText(expiresTo) || hasText(firstUsedFrom) || hasText(firstUsedTo);

        LocalDateTime expiresFromDt = parseDate(expiresFrom, false);
        LocalDateTime expiresToDt = parseDate(expiresTo, true);
        LocalDateTime firstUsedFromDt = parseDate(firstUsedFrom, false);
        LocalDateTime firstUsedToDt = parseDate(firstUsedTo, true);

        Page<Voucher> voucherPage = voucherService.findVouchers(
            PageRequest.of(page, PAGE_SIZE, Sort.by(direction, sortBy)),
            voucherSearch,
            patronId,
            effectiveStatuses,
            type,
            discountType,
            expiresFromDt,
            expiresToDt,
            firstUsedFromDt,
            firstUsedToDt
        );

        model.addAttribute("pageTitle", messageSource.getMessage("voucher.admin.title", null, locale));
        model.addAttribute("voucherPage", voucherPage);
        model.addAttribute("displayPageNumber", voucherPage.getNumber() + 1);
        model.addAttribute("displayTotalPages", Math.max(voucherPage.getTotalPages(), 1));
        model.addAttribute("displayedFrom", voucherPage.hasContent()
            ? (long) voucherPage.getNumber() * voucherPage.getSize() + 1
            : 0);
        model.addAttribute("displayedTo", voucherPage.hasContent()
            ? (long) voucherPage.getNumber() * voucherPage.getSize() + voucherPage.getNumberOfElements()
            : 0);
        model.addAttribute("voucherSearch", voucherSearch);
        model.addAttribute("patronId", patronId);
        model.addAttribute("statuses", statuses);
        model.addAttribute("type", type);
        model.addAttribute("discountType", discountType);
        model.addAttribute("expiresFrom", expiresFrom);
        model.addAttribute("expiresTo", expiresTo);
        model.addAttribute("firstUsedFrom", firstUsedFrom);
        model.addAttribute("firstUsedTo", firstUsedTo);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("voucherStatuses", VoucherStatus.values());
        model.addAttribute("voucherTypes", VoucherType.values());
        model.addAttribute("voucherRecipientTypes", VoucherRecipientType.values());
        model.addAttribute("voucherPrinterConfigured", this.appSettingsService.hasVoucherPrinterAppUrl());
        model.addAttribute("maxBatchPrintSize", MAX_BATCH_PRINT_SIZE);
        model.addAttribute("statusFilterActive", statusFilterActive);
        model.addAttribute("activeFilters", activeFilters);
        model.addAttribute("voucherStatusStyleMap", Map.of(
            VoucherStatus.ACTIVE.name(), "bg-green-100 text-green-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm",
            VoucherStatus.INACTIVE.name(), "bg-red-100 text-red-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm",
            VoucherStatus.EXPIRED.name(), "bg-gray-100 text-gray-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm",
            VoucherStatus.APPLIED.name(), "bg-yellow-100 text-yellow-800 text-xs font-medium me-2 px-2.5 py-0.5 rounded-sm"
        ));

        return "vouchers";
    }

    /**
     * Voucher creation / bulk-generate forms page
     */
    @RequestMapping(value = "/dashboard/vouchers/new", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String newVoucherForm(Model model, Locale locale) {
        model.addAttribute("pageTitle", messageSource.getMessage("voucher.admin.newVoucher", null, locale));
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("voucherTypes", VoucherType.values());
        model.addAttribute("voucherRecipientTypes", VoucherRecipientType.values());
        return "vouchers_create";
    }

    /**
     * View a single voucher's details and usage history
     */
    @RequestMapping(value = "/dashboard/vouchers/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String viewVoucher(
        @PathVariable Long id,
        Model model,
        Locale locale
    ) {
        Optional<Voucher> voucherOpt = voucherService.findById(id);
        if (!voucherOpt.isPresent()) {
            return "redirect:/dashboard/vouchers";
        }

        Voucher voucher = voucherOpt.get();
        List<VoucherUsage> usages = voucherService.getUsagesByVoucher(voucher);

        model.addAttribute("pageTitle", messageSource.getMessage("voucher.admin.detail.title", new Object[]{voucher.getCode()}, locale));
        model.addAttribute("voucher", voucher);
        model.addAttribute("usages", usages);
        model.addAttribute("voucherRecipientTypes", VoucherRecipientType.values());

        return "voucher_detail";
    }

    /**
     * Redirect to the external Voucher printer app with real voucher data.
     */
    @RequestMapping(value = "/dashboard/vouchers/{id}/print", method = RequestMethod.GET)
    public String printVoucher(
        @PathVariable Long id,
        RedirectAttributes redirectAttributes,
        Locale locale
    ) {
        Optional<Voucher> voucherOpt = voucherService.findById(id);
        if (!voucherOpt.isPresent()) {
            return "redirect:/dashboard/vouchers";
        }

        Optional<String> printerUrl = this.voucherPrinterService.buildPrinterUrl(voucherOpt.get());
        if (!printerUrl.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                messageSource.getMessage("voucher.admin.printerUrlMissing", null, locale));
            return "redirect:/dashboard/vouchers";
        }

        return "redirect:" + printerUrl.get();
    }

    /**
     * Opens the external Voucher printer app with selected vouchers as a batch payload.
     */
    @RequestMapping(value = "/dashboard/vouchers/print-selected", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    public String printSelectedVouchers(
        @RequestParam(required = false) List<Long> voucherIds,
        @RequestParam(required = false) String selectionMode,
        @RequestParam(required = false) String voucherSearch,
        @RequestParam(required = false) String patronId,
        @RequestParam(required = false) List<VoucherStatus> statuses,
        @RequestParam(required = false) VoucherType type,
        @RequestParam(required = false) DiscountType discountType,
        @RequestParam(required = false) String expiresFrom,
        @RequestParam(required = false) String expiresTo,
        @RequestParam(required = false) String firstUsedFrom,
        @RequestParam(required = false) String firstUsedTo,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDir,
        Model model,
        RedirectAttributes redirectAttributes,
        Locale locale
    ) {
        Optional<String> printerAppUrl = this.voucherPrinterService.getPrinterAppUrl();
        if (!printerAppUrl.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                messageSource.getMessage("voucher.admin.printerUrlMissing", null, locale));
            return "redirect:/dashboard/vouchers";
        }

        if ("FILTERED".equals(selectionMode)) {
            Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            List<VoucherStatus> effectiveStatuses = (statuses == null || statuses.isEmpty() ||
                statuses.size() == VoucherStatus.values().length) ? Arrays.asList(VoucherStatus.values()) : statuses;

            List<VoucherPrintData> printableVouchers = this.voucherService.findVoucherPrintData(
                PageRequest.of(0, MAX_BATCH_PRINT_SIZE + 1, Sort.by(direction, sortBy)),
                voucherSearch,
                patronId,
                effectiveStatuses,
                type,
                discountType,
                parseDate(expiresFrom, false),
                parseDate(expiresTo, true),
                parseDate(firstUsedFrom, false),
                parseDate(firstUsedTo, true)
            );

            if (printableVouchers.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("voucher.admin.printSelectedNone", null, locale));
                return "redirect:/dashboard/vouchers";
            }
            if (printableVouchers.size() > MAX_BATCH_PRINT_SIZE) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("voucher.admin.printSelectedTooMany", new Object[]{MAX_BATCH_PRINT_SIZE}, locale));
                return "redirect:/dashboard/vouchers";
            }

            model.addAttribute("pageTitle", messageSource.getMessage("voucher.admin.printSelectedOpening", null, locale));
            model.addAttribute("voucherPrinterAppUrl", printerAppUrl.get());
            model.addAttribute("payload", this.voucherPrinterService.buildBatchPayloadFromPrintData(printableVouchers));
            return "voucher_printer_post";
        }

        if (voucherIds == null || voucherIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                messageSource.getMessage("voucher.admin.printSelectedNone", null, locale));
            return "redirect:/dashboard/vouchers";
        }

        if (voucherIds.size() > MAX_BATCH_PRINT_SIZE) {
            redirectAttributes.addFlashAttribute("errorMessage",
                messageSource.getMessage("voucher.admin.printSelectedTooMany", new Object[]{MAX_BATCH_PRINT_SIZE}, locale));
            return "redirect:/dashboard/vouchers";
        }

        List<Voucher> foundVouchers = this.voucherService.findAllByIds(voucherIds);
        if (foundVouchers.size() != voucherIds.size()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                messageSource.getMessage("voucher.admin.printSelectedMissing", null, locale));
            return "redirect:/dashboard/vouchers";
        }

        Map<Long, Voucher> vouchersById = new LinkedHashMap<>();
        for (Voucher voucher : foundVouchers) {
            vouchersById.put(voucher.getId(), voucher);
        }

        List<Voucher> orderedVouchers = new ArrayList<>();
        for (Long voucherId : voucherIds) {
            orderedVouchers.add(vouchersById.get(voucherId));
        }

        model.addAttribute("pageTitle", messageSource.getMessage("voucher.admin.printSelectedOpening", null, locale));
        model.addAttribute("voucherPrinterAppUrl", printerAppUrl.get());
        model.addAttribute("payload", this.voucherPrinterService.buildBatchPayload(orderedVouchers));
        return "voucher_printer_post";
    }

    /**
     * Create a single voucher
     */
    @RequestMapping(value = "/dashboard/vouchers/create", method = RequestMethod.POST)
    public String createVoucher(
        @RequestParam(required = false) String code,
        @RequestParam DiscountType discountType,
        @RequestParam BigDecimal discountValue,
        @RequestParam(defaultValue = "1") int maxUses,
        @RequestParam(required = false) String expiresAt,
        @RequestParam(required = false) String note,
        @RequestParam(defaultValue = "DIGITAL") VoucherType type,
        @RequestParam(required = false) VoucherRecipientType recipientType,
        @RequestParam(required = false) String buyer,
        @RequestParam(required = false) String paymentMethod,
        @RequestParam(required = false) String invoice,
        RedirectAttributes redirectAttributes,
        Locale locale
    ) {
        try {
            if (!isAllowedIssuanceDiscount(discountType, discountValue)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("voucher.admin.partialDiscountsNotAllowed", null, locale));
                return "redirect:/dashboard/vouchers/new";
            }

            String voucherCode = (code != null && !code.trim().isEmpty()) ? code.trim().toUpperCase() : voucherService.generateUniqueCode();
            Voucher voucher = new Voucher(voucherCode, discountType, discountValue, maxUses);
            if (expiresAt != null && !expiresAt.isEmpty()) {
                voucher.setExpiresAt(LocalDateTime.parse(expiresAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            voucher.setNote(note);
            voucher.setType(type);
            voucher.setRecipientType(recipientType);
            voucher.setBuyer(buyer);
            voucher.setPaymentMethod(paymentMethod);
            voucher.setInvoice(invoice);
            voucherService.save(voucher);
            redirectAttributes.addFlashAttribute("successMessage", messageSource.getMessage("voucher.admin.created", null, locale));
        } catch (Exception e) {
            getLogger().error("Failed to create voucher: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", messageSource.getMessage("voucher.admin.createFailed", null, locale));
        }
        return "redirect:/dashboard/vouchers/new";
    }

    /**
     * Bulk generate vouchers
     */
    @RequestMapping(value = "/dashboard/vouchers/bulk-generate", method = RequestMethod.POST)
    public String bulkGenerate(
        @RequestParam int count,
        @RequestParam DiscountType discountType,
        @RequestParam BigDecimal discountValue,
        @RequestParam(defaultValue = "1") int maxUses,
        @RequestParam(required = false) String expiresAt,
        @RequestParam(required = false) String note,
        @RequestParam(defaultValue = "DIGITAL") VoucherType type,
        @RequestParam(required = false) VoucherRecipientType recipientType,
        @RequestParam(required = false) String buyer,
        @RequestParam(required = false) String paymentMethod,
        @RequestParam(required = false) String invoice,
        RedirectAttributes redirectAttributes,
        Locale locale
    ) {
        try {
            if (!isAllowedIssuanceDiscount(discountType, discountValue)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("voucher.admin.partialDiscountsNotAllowed", null, locale));
                return "redirect:/dashboard/vouchers/new";
            }

            LocalDateTime expiry = null;
            if (expiresAt != null && !expiresAt.isEmpty()) {
                expiry = LocalDateTime.parse(expiresAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            List<Voucher> vouchers = voucherService.bulkGenerate(count, discountType, discountValue, maxUses, expiry, note, type, recipientType, buyer, paymentMethod, invoice);
            String codes = vouchers.stream().map(Voucher::getCode).collect(Collectors.joining(", "));
            redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("voucher.admin.bulkGenerated", new Object[]{vouchers.size()}, locale));
            redirectAttributes.addFlashAttribute("generatedCodes", codes);
        } catch (Exception e) {
            getLogger().error("Failed to bulk generate vouchers: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", messageSource.getMessage("voucher.admin.bulkGenerateFailed", null, locale));
        }
        return "redirect:/dashboard/vouchers/new";
    }

    /**
     * Update editable fields on a voucher (note, buyer, paymentMethod, invoice)
     */
    @RequestMapping(value = "/dashboard/vouchers/{id}/update", method = RequestMethod.POST)
    public String updateVoucher(
        @PathVariable Long id,
        @RequestParam(required = false) String note,
        @RequestParam(required = false) String buyer,
        @RequestParam(required = false) String paymentMethod,
        @RequestParam(required = false) String invoice,
        @RequestParam(required = false) VoucherRecipientType recipientType,
        RedirectAttributes redirectAttributes,
        Locale locale
    ) {
        Optional<Voucher> voucherOpt = voucherService.findById(id);
        if (!voucherOpt.isPresent()) {
            return "redirect:/dashboard/vouchers";
        }

        Voucher voucher = voucherOpt.get();
        voucher.setNote(trimToNull(note));
        voucher.setBuyer(trimToNull(buyer));
        voucher.setPaymentMethod(trimToNull(paymentMethod));
        voucher.setInvoice(trimToNull(invoice));
        voucher.setRecipientType(recipientType);

        voucherService.save(voucher);
        redirectAttributes.addFlashAttribute("successMessage", messageSource.getMessage("voucher.admin.updated", null, locale));
        return "redirect:/dashboard/vouchers/" + id;
    }

    private static String trimToNull(String value) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Issuance policy only: when partial discounts are disabled, newly-created
     * vouchers must fully cover the configured registration/renewal fee.
     */
    private boolean isAllowedIssuanceDiscount(DiscountType discountType, BigDecimal discountValue) {
        if (voucherIssuanceConfig.isAllowPartialDiscounts()) {
            return true;
        }

        BigDecimal feeAmount = registrationFeeConfig.getDefaultAmount();
        Voucher candidate = new Voucher("POLICY-CHECK", discountType, discountValue, 1);
        BigDecimal effectiveDiscount = voucherService.computeDiscount(candidate, feeAmount);
        return effectiveDiscount.compareTo(feeAmount) >= 0;
    }

    /**
     * Deactivate a voucher (soft delete)
     */
    @RequestMapping(value = "/dashboard/vouchers/{id}/deactivate", method = RequestMethod.GET)
    public String deactivateVoucher(
        @PathVariable Long id,
        RedirectAttributes redirectAttributes,
        Locale locale
    ) {
        voucherService.deactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", messageSource.getMessage("voucher.admin.deactivated", null, locale));
        return "redirect:/dashboard/vouchers";
    }

    /**
     * Deactivate selected active vouchers.
     */
    @RequestMapping(value = "/dashboard/vouchers/deactivate-selected", method = RequestMethod.POST)
    public String deactivateSelectedVouchers(
        @RequestParam(required = false) List<Long> voucherIds,
        RedirectAttributes redirectAttributes,
        Locale locale
    ) {
        if (voucherIds == null || voucherIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                messageSource.getMessage("voucher.admin.deactivateSelectedNone", null, locale));
            return "redirect:/dashboard/vouchers";
        }

        int deactivatedCount = voucherService.deactivateActiveVouchers(voucherIds);
        redirectAttributes.addFlashAttribute("successMessage",
            messageSource.getMessage("voucher.admin.deactivateSelectedDone", new Object[]{deactivatedCount}, locale));
        return "redirect:/dashboard/vouchers";
    }

    /**
     * Reactivate a voucher
     */
    @RequestMapping(value = "/dashboard/vouchers/{id}/activate", method = RequestMethod.GET)
    public String activateVoucher(
        @PathVariable Long id,
        RedirectAttributes redirectAttributes,
        Locale locale
    ) {
        voucherService.activate(id);
        redirectAttributes.addFlashAttribute("successMessage", messageSource.getMessage("voucher.admin.activated", null, locale));
        return "redirect:/dashboard/vouchers";
    }

    /**
     * Export filtered vouchers as CSV
     */
    @RequestMapping(value = "/dashboard/vouchers/export", method = RequestMethod.GET, produces = "text/csv")
    public void exportVouchersCsv(
        HttpServletResponse response,
        @RequestParam(required = false) String voucherSearch,
        @RequestParam(required = false) String patronId,
        @RequestParam(required = false) List<VoucherStatus> statuses,
        @RequestParam(required = false) VoucherType type,
        @RequestParam(required = false) DiscountType discountType,
        @RequestParam(required = false) String expiresFrom,
        @RequestParam(required = false) String expiresTo,
        @RequestParam(required = false) String firstUsedFrom,
        @RequestParam(required = false) String firstUsedTo,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDir
    ) throws IOException {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        List<VoucherStatus> effectiveStatuses = (statuses == null || statuses.isEmpty() ||
            statuses.size() == VoucherStatus.values().length) ? Arrays.asList(VoucherStatus.values()) : statuses;

        LocalDateTime expiresFromDt = parseDate(expiresFrom, false);
        LocalDateTime expiresToDt = parseDate(expiresTo, true);
        LocalDateTime firstUsedFromDt = parseDate(firstUsedFrom, false);
        LocalDateTime firstUsedToDt = parseDate(firstUsedTo, true);

        // Fetch all matching vouchers (no pagination)
        Page<Voucher> allVouchers = voucherService.findVouchers(
            PageRequest.of(0, Integer.MAX_VALUE, Sort.by(direction, sortBy)),
            voucherSearch,
            patronId,
            effectiveStatuses,
            type,
            discountType,
            expiresFromDt,
            expiresToDt,
            firstUsedFromDt,
            firstUsedToDt
        );

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"vouchers.csv\"");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (PrintWriter writer = response.getWriter()) {
            // BOM for Excel UTF-8 recognition
            writer.print('\uFEFF');
            writer.println("ID,Code,Type,Recipient Type,Status,Discount Type,Discount Value,Uses,Max Uses,Expires At,First Used At,Patron IDs,Note,Buyer,Payment Method,Invoice,Created At");

            for (Voucher v : allVouchers.getContent()) {
                writer.println(String.join(",",
                    escapeCsv(String.valueOf(v.getId())),
                    escapeCsv(v.getCode()),
                    escapeCsv(v.getType() != null ? v.getType().name() : ""),
                    escapeCsv(v.getRecipientType() != null ? v.getRecipientType().name() : ""),
                    escapeCsv(v.getStatus() != null ? v.getStatus().name() : ""),
                    escapeCsv(v.getDiscountType() != null ? v.getDiscountType().name() : ""),
                    escapeCsv(v.getDiscountValue() != null ? v.getDiscountValue().toPlainString() : ""),
                    escapeCsv(String.valueOf(v.getCurrentUses())),
                    escapeCsv(String.valueOf(v.getMaxUses())),
                    escapeCsv(v.getExpiresAt() != null ? v.getExpiresAt().format(dtf) : ""),
                    escapeCsv(v.getFirstUsedAt() != null ? v.getFirstUsedAt().format(dtf) : ""),
                    escapeCsv(v.getPatronIds() != null ? v.getPatronIds() : ""),
                    escapeCsv(v.getNote() != null ? v.getNote() : ""),
                    escapeCsv(v.getBuyer() != null ? v.getBuyer() : ""),
                    escapeCsv(v.getPaymentMethod() != null ? v.getPaymentMethod() : ""),
                    escapeCsv(v.getInvoice() != null ? v.getInvoice() : ""),
                    escapeCsv(v.getCreatedAt() != null ? v.getCreatedAt().format(dtf) : "")
                ));
            }
        }
    }

    private static LocalDateTime parseDate(String dateStr, boolean endOfDay) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        LocalDate date = LocalDate.parse(dateStr.trim());
        return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
    }

    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
