package cz.cas.lib.bankid_registrator.model.voucher;

import cz.cas.lib.bankid_registrator.entities.voucher.DiscountType;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherRecipientType;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherStatus;
import cz.cas.lib.bankid_registrator.entities.voucher.VoucherType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Voucher entity represents a discount code (coupon) that can reduce
 * the registration or renewal fee.
 */
@Entity
@Table(name = "voucher")
@Getter
@Setter
public class Voucher
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique voucher code (the string entered by the patron)
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Type of discount: PERCENTAGE or FIXED_CZK
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    /**
     * Discount value:
     * - If PERCENTAGE: 0–100 (e.g. 50 = 50% off)
     * - If FIXED_CZK: amount in CZK (e.g. 140 = 140 CZK off)
     */
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /**
     * Maximum number of times this voucher can be used (0 = unlimited)
     */
    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    /**
     * Current number of times this voucher has been used (confirmed uses only)
     */
    @Column(name = "current_uses", nullable = false)
    private int currentUses;

    /**
     * Optional expiration date. Null means the voucher never expires.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Voucher status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'")
    private VoucherStatus status;

    /**
     * Voucher type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'DIGITAL'")
    private VoucherType type;

    /**
     * Optional recipient/customer category for physical voucher rendering.
     * Null means a standard voucher with no special printer rendering.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = true)
    private VoucherRecipientType recipientType;

    /**
     * Optional note for admins (e.g. reason for creation, target audience)
     */
    @Column(name = "note", length = 500)
    private String note;

    /**
     * Optional buyer name/identifier
     */
    @Column(name = "buyer", length = 500)
    private String buyer;

    /**
     * Optional payment method used to purchase this voucher
     */
    @Column(name = "payment_method", length = 500)
    private String paymentMethod;

    /**
     * Optional invoice number/reference
     */
    @Column(name = "invoice", length = 500)
    private String invoice;

    /**
     * Timestamp of the first confirmed usage of this voucher.
     * Denormalized for efficient sorting in the admin list.
     */
    @Column(name = "first_used_at")
    private LocalDateTime firstUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoucherUsage> usages = new ArrayList<>();

    public Voucher() {
        this.status = VoucherStatus.ACTIVE;
        this.type = VoucherType.DIGITAL;
        this.currentUses = 0;
    }

    public Voucher(String code, DiscountType discountType, BigDecimal discountValue, int maxUses) {
        this();
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxUses = maxUses;
    }

    /**
     * Convenience method: returns true if the voucher status is ACTIVE
     */
    public boolean isActive() {
        return this.status == VoucherStatus.ACTIVE;
    }

    /**
     * Compute patron IDs from confirmed usages (replaces the former denormalized column)
     */
    @Transient
    public String getPatronIds() {
        if (this.usages == null) {
            return null;
        }
        String ids = this.usages.stream()
            .filter(VoucherUsage::isConfirmed)
            .map(VoucherUsage::getPatronAlephId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.joining(", "));
        return ids.isEmpty() ? null : ids;
    }
}
