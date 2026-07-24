package cz.cas.lib.bankid_registrator.model.voucher;

import cz.cas.lib.bankid_registrator.model.identity.Identity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * VoucherUsage records each use of a voucher by a patron.
 * One voucher can be used multiple times (up to maxUses), but each patron
 * can only use a given voucher once.
 */
@Entity
@Table(name = "voucher_usage")
@Getter
@Setter
public class VoucherUsage
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "identity_id", nullable = false)
    private Identity identity;

    /**
     * The Aleph patron ID at the time of usage (for audit trail)
     */
    @Column(name = "patron_aleph_id", length = 20)
    private String patronAlephId;

    /**
     * The actual discount amount applied in CZK (after computation)
     */
    @Column(name = "discount_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountApplied;

    /**
     * Whether this usage has been confirmed (payment succeeded).
     * Pending usages are created when the voucher is applied,
     * and confirmed when the payment succeeds.
     */
    @Column(name = "confirmed", nullable = false)
    private boolean confirmed;

    @CreationTimestamp
    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;

    public VoucherUsage() {
        this.confirmed = false;
    }

    public VoucherUsage(Voucher voucher, Identity identity, String patronAlephId, BigDecimal discountApplied) {
        this();
        this.voucher = voucher;
        this.identity = identity;
        this.patronAlephId = patronAlephId;
        this.discountApplied = discountApplied;
    }
}
