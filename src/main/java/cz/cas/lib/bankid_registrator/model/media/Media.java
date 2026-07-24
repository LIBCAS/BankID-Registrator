package cz.cas.lib.bankid_registrator.model.media;

import cz.cas.lib.bankid_registrator.entities.media.MediaSubmissionType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import java.time.LocalDateTime;
import javax.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "media")
public class Media
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String path;

    @Column(name = "original_name")
    private String originalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type")
    private MediaSubmissionType submissionType;

    @Column(name = "submission_batch_id")
    private String submissionBatchId;

    @Column(name = "batch_order_index")
    private Integer batchOrderIndex;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "identity_id")
    private Identity identity;

    public Long getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getDisplayName() {
        return originalName != null && !originalName.trim().isEmpty() ? originalName : name;
    }

    public void setSubmissionType(MediaSubmissionType submissionType) {
        this.submissionType = submissionType;
    }

    public MediaSubmissionType getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionBatchId(String submissionBatchId) {
        this.submissionBatchId = submissionBatchId;
    }

    public String getSubmissionBatchId() {
        return submissionBatchId;
    }

    public void setBatchOrderIndex(Integer batchOrderIndex) {
        this.batchOrderIndex = batchOrderIndex;
    }

    public Integer getBatchOrderIndex() {
        return batchOrderIndex;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public Identity getIdentity() {
        return this.identity;
    }
}
