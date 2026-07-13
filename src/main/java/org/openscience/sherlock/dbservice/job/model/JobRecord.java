package org.openscience.sherlock.dbservice.job.model;

import java.time.OffsetDateTime;

import org.openscience.sherlock.utils.elucidation.job.JobState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
public class JobRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String jobId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private JobState state;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "process_id")
    private Long processId;

    @Column(name = "request_data", nullable = false, columnDefinition = "TEXT")
    private String requestData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private OffsetDateTime lastModifiedAt;

    @PrePersist
    void onCreate() {
        final OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        lastModifiedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        lastModifiedAt = OffsetDateTime.now();
    }

}