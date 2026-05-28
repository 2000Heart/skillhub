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
@Table(name = "org_sync_state")
public class OrgSyncState {
    @Id
    @Column(name = "sync_key", length = 64)
    private String syncKey;

    @Column(name = "phase", nullable = false, length = 64)
    private String phase;

    @Column(name = "cursor_value", length = 256)
    private String cursorValue;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrgSyncState() {}

    public OrgSyncState(String syncKey, String phase) {
        this.syncKey = syncKey;
        this.phase = phase;
    }

    @PrePersist
    void prePersist() {
        this.updatedAt = Instant.now(Clock.systemUTC());
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now(Clock.systemUTC());
    }

    public String getSyncKey() { return syncKey; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getCursorValue() { return cursorValue; }
    public void setCursorValue(String cursorValue) { this.cursorValue = cursorValue; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(Instant lastRunAt) { this.lastRunAt = lastRunAt; }
}
