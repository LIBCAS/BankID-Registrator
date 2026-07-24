package cz.cas.lib.bankid_registrator.entities.payment;

import java.util.Locale;
import java.util.Optional;

public enum ComgateReturnStatus {
    SUCCESS("success"),
    CANCELLED("cancelled"),
    PENDING("pending");

    private final String value;

    ComgateReturnStatus(String value) {
        this.value = value;
    }

    public static Optional<ComgateReturnStatus> fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);

        for (ComgateReturnStatus status : values()) {
            if (status.value.equals(normalizedValue)) {
                return Optional.of(status);
            }
        }

        return Optional.empty();
    }
}
