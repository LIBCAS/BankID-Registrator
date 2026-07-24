package cz.cas.lib.bankid_registrator.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.bankid_registrator.configurations.RegistrationFeeConfig;
import cz.cas.lib.bankid_registrator.dto.VoucherPrintData;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class VoucherPrinterService
{
    private static final DateTimeFormatter VOUCHER_PRINTER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppSettingsService appSettingsService;
    private final RegistrationFeeConfig registrationFeeConfig;
    private final VoucherService voucherService;
    private final ObjectMapper objectMapper;

    public VoucherPrinterService(
        AppSettingsService appSettingsService,
        RegistrationFeeConfig registrationFeeConfig,
        VoucherService voucherService,
        ObjectMapper objectMapper
    ) {
        this.appSettingsService = appSettingsService;
        this.registrationFeeConfig = registrationFeeConfig;
        this.voucherService = voucherService;
        this.objectMapper = objectMapper;
    }

    public Optional<String> getPrinterAppUrl() {
        return this.appSettingsService.getVoucherPrinterAppUrl();
    }

    public Optional<String> buildPrinterUrl(Voucher voucher) {
        Optional<String> printerAppUrl = this.appSettingsService.getVoucherPrinterAppUrl();
        if (!printerAppUrl.isPresent()) {
            return Optional.empty();
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(printerAppUrl.get())
            .queryParam("code", voucher.getCode())
            .queryParam("issuedat", voucher.getCreatedAt().format(VOUCHER_PRINTER_DATE_FORMAT))
            .queryParam("discount", formatDiscountAmount(computeNominalDiscount(voucher)));

        if (voucher.getExpiresAt() != null) {
            builder.queryParam("expiresat", voucher.getExpiresAt().format(VOUCHER_PRINTER_DATE_FORMAT));
        }

        String printerRecipientType = getVoucherPrinterRecipientType(voucher);
        if (!printerRecipientType.isEmpty()) {
            builder.queryParam("recipient_type", printerRecipientType);
        }

        return Optional.of(builder.build().encode().toUriString());
    }

    public String buildBatchPayload(List<Voucher> vouchers) {
        Map<String, Object> payload = new LinkedHashMap<>();
        List<Map<String, Object>> voucherPayloads = new ArrayList<>();
        for (Voucher voucher : vouchers) {
            voucherPayloads.add(buildVoucherPayload(voucher));
        }
        payload.put("vouchers", voucherPayloads);

        try {
            return this.objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build Voucher printer payload.", e);
        }
    }

    public String buildBatchPayloadFromPrintData(List<VoucherPrintData> vouchers) {
        Map<String, Object> payload = new LinkedHashMap<>();
        List<Map<String, Object>> voucherPayloads = new ArrayList<>();
        for (VoucherPrintData voucher : vouchers) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", voucher.getCode());
            item.put("issuedat", voucher.getCreatedAt() != null ? voucher.getCreatedAt().format(VOUCHER_PRINTER_DATE_FORMAT) : "");
            item.put("expiresat", voucher.getExpiresAt() != null ? voucher.getExpiresAt().format(VOUCHER_PRINTER_DATE_FORMAT) : "");
            item.put("discount", computeNominalDiscount(voucher).stripTrailingZeros());
            item.put("recipient_type", voucher.getRecipientType() != null ? voucher.getRecipientType().name() : "");
            voucherPayloads.add(item);
        }
        payload.put("vouchers", voucherPayloads);

        try {
            return this.objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build Voucher printer payload.", e);
        }
    }

    private Map<String, Object> buildVoucherPayload(Voucher voucher) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", voucher.getCode());
        item.put("issuedat", voucher.getCreatedAt() != null ? voucher.getCreatedAt().format(VOUCHER_PRINTER_DATE_FORMAT) : "");
        item.put("expiresat", voucher.getExpiresAt() != null ? voucher.getExpiresAt().format(VOUCHER_PRINTER_DATE_FORMAT) : "");
        item.put("discount", computeNominalDiscount(voucher).stripTrailingZeros());
        item.put("recipient_type", getVoucherPrinterRecipientType(voucher));
        return item;
    }

    private BigDecimal computeNominalDiscount(Voucher voucher) {
        return this.voucherService.computeDiscount(voucher, this.registrationFeeConfig.getDefaultAmount());
    }

    private BigDecimal computeNominalDiscount(VoucherPrintData voucher) {
        return this.voucherService.computeDiscount(
            voucher.getDiscountType(),
            voucher.getDiscountValue(),
            this.registrationFeeConfig.getDefaultAmount()
        );
    }

    private String formatDiscountAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private String getVoucherPrinterRecipientType(Voucher voucher) {
        return voucher.getRecipientType() != null ? voucher.getRecipientType().name() : "";
    }
}
