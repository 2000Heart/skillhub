package com.iflytek.skillhub.domain.orgsync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;

@Entity
@Table(name = "org_event_checkpoint")
public class OrgEventCheckpoint {
    @Id
    @Column(name = "event_key", length = 256)
    private String eventKey;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "error_message")
    private String errorMessage;

    protected OrgEventCheckpoint() {}

    public OrgEventCheckpoint(String eventKey, String eventType, String status) {
        this.eventKey = eventKey;
        this.eventType = eventType;
        this.status = status;
    }

    @PrePersist
    void prePersist() {
        if (this.receivedAt == null) {
            this.receivedAt = Instant.now(Clock.systemUTC());
        }
    }

    @PreUpdate
    void preUpdate() {
        // no-op
    }

    public String getEventKey() { return eventKey; }
    public String getEventType() { return eventType; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
