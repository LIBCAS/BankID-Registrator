package cz.cas.lib.bankid_registrator.model.identity;

import cz.cas.lib.bankid_registrator.entities.activity.ActivityEvent;
import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "identity_activity")
@Getter
@Setter
public class IdentityActivity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "identity_id", nullable = false)
    private Identity identity;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_event", nullable = false)
    private ActivityEvent activityEvent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public IdentityActivity(Identity identity, ActivityEvent activityEvent) {
        this.identity = identity;
        this.activityEvent = activityEvent;
    }
}