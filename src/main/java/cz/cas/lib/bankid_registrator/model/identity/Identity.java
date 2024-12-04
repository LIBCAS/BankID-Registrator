package cz.cas.lib.bankid_registrator.model.identity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.cas.lib.bankid_registrator.model.media.Media;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Identity is a user who has successfully verified their identity via BankID
 */
@Entity
@Table(name = "identity")
@Getter
@Setter
public class Identity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id", nullable = false, unique = true)
    private String bankId;

    @Column(name = "aleph_id", nullable = true, unique = true)
    private String alephId;

    @Column(name = "aleph_barcode", nullable = true, unique = true)
    private String alephBarcode;

    @Column(name="is_cas_employee", nullable = true)
    private Boolean isCasEmployee;

    @Column(name="checked_by_admin", nullable = true)
    private Boolean checkedByAdmin;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "identity", cascade = CascadeType.ALL)
    private List<Media> media = new ArrayList<>();

    public Identity() {
    }

    public Identity(String bankId) {
        this.bankId = bankId;
    }
}
