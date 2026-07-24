package cz.cas.lib.bankid_registrator.model.payment;

import cz.cas.lib.bankid_registrator.entities.payment.PaymentStatus;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Payment entity represents a payment transaction for registration or renewal fee
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
public class Payment
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "identity_id", nullable = false)
    private Identity identity;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    /**
     * Total amount from Aleph (before any voucher discount)
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Voucher code applied to this payment (null if no voucher used)
     */
    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    /**
     * Discount amount in CZK applied via the voucher (0 if no voucher)
     */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Payment() {
        this.discountAmount = BigDecimal.ZERO;
    }

    public Payment(Identity identity, PaymentType type, PaymentStatus status, BigDecimal amount) {
        this();
        this.identity = identity;
        this.type = type;
        this.status = status;
        this.amount = amount;
    }

    /**
     * Get the amount the patron actually needs to pay (amount minus discount)
     */
    public BigDecimal getAmountToPay() {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        BigDecimal result = amount.subtract(discountAmount);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }
}
