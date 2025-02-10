package cz.cas.lib.bankid_registrator.entities.patron;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum PatronFineStatus {
    PAID("C", "paid"),
    NOT_PAID("O", "notpaid"),
    CANCELLED("W", "cancelled"),
    UNKNOWN("", "unknown");

    private final String code;
    private final String key;
    private static final Map<String, PatronFineStatus> CODE_TO_STATUS_MAP = Stream.of(values()).collect(Collectors.toMap(PatronFineStatus::getCode, status -> status));

    PatronFineStatus(String code, String key) {
        this.code = code;
        this.key = key;
    }

    public String getCode() {
        return code;
    }

    public String getKey() {
        return key;
    }

    public static PatronFineStatus getByCode(String code) {
        return CODE_TO_STATUS_MAP.getOrDefault(code, UNKNOWN);
    }
}
