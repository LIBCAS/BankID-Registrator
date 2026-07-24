package cz.cas.lib.bankid_registrator.model.test_settings;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Singleton entity (always id=1) storing test settings for the Tester's Toolkit.
 * Used in local and testing environments to allow testers to dynamically change
 * the fake BankID identity middle name and force-enable renewal mode.
 */
@Entity
@Table(name = "test_settings")
@Getter
@Setter
public class TestSettings
{
    @Id
    private Long id;

    @Column(name = "middle_name", nullable = false, length = 100)
    private String middleName;

    @Column(name = "tester_email", length = 255)
    private String testerEmail;

    @Column(name = "force_renewal", nullable = false)
    private boolean forceRenewal;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TestSettings() {
        this.id = 1L;
        this.middleName = "Tester";
        this.testerEmail = null;
        this.forceRenewal = false;
    }
}
