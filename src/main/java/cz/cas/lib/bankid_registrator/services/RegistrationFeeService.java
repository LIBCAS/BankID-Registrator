package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.RegistrationFeeConfig;
import cz.cas.lib.bankid_registrator.entities.patron.PatronStatus;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Resolves the fee tariff; actual outstanding balances remain authoritative in Aleph. */
@Service
public class RegistrationFeeService {
    private final RegistrationFeeConfig config;
    private final AlephService alephService;
    private final PatronService patronService;

    public RegistrationFeeService(RegistrationFeeConfig config, AlephService alephService,
                                  PatronService patronService) {
        this.config = config;
        this.alephService = alephService;
        this.patronService = patronService;
    }

    public BigDecimal getFee(Identity identity) {
        return getFee(loadPatron(identity));
    }

    public BigDecimal getFee(Patron patron) {
        return amountForStatus(PatronStatus.getById(patron.getStatus()));
    }

    public BigDecimal getRenewalFee(Patron patron) {
        // Resolve before extending expiry: retirement eligibility uses the next membership start.
        return amountForStatus(patronService.determinePatronStatus(patron));
    }

    public BigDecimal getPreviewFee(Identity identity, boolean renewalForm) {
        Patron patron = loadPatron(identity);
        return renewalForm ? getRenewalFee(patron) : getFee(patron);
    }

    private Patron loadPatron(Identity identity) {
        Map<String, Object> result = alephService.getAlephPatron(identity.getAlephId(), false);
        if (result == null || result.containsKey("error") || !(result.get("patron") instanceof Patron)) {
            throw new IllegalStateException("Cannot resolve the patron's registration fee from Aleph");
        }
        return (Patron) result.get("patron");
    }

    private BigDecimal amountForStatus(PatronStatus status) {
        if (status == null) {
            throw new IllegalStateException("Cannot resolve fee for an unknown patron status");
        }
        if (status.isEmployee()) {
            return BigDecimal.ZERO;
        }
        return status == PatronStatus.STATUS_10 || status == PatronStatus.STATUS_15
            ? config.getSeniorAmount() : config.getDefaultAmount();
    }
}
