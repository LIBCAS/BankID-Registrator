package cz.cas.lib.bankid_registrator.model.voucher;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A single transactional counter; deliberately does not use AUTO_INCREMENT. */
@Entity
@Table(name = "voucher_accounting_counter")
@Getter
@Setter
public class VoucherAccountingCounter {
    @Id
    private Long id;

    @Column(name = "last_number", nullable = false)
    private long lastNumber;
}
