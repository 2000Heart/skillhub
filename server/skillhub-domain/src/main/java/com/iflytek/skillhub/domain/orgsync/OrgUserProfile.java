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
@Table(name = "org_user_profile")
public class OrgUserProfile {
    @Id
    @Column(name = "user_id", length = 128)
    private String userId;

    @Column(name = "union_id", length = 128)
    private String unionId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "title", length = 128)
    private String title;

    @Column(name = "mobile", length = 64)
    private String mobile;

    @Column(name = "email", length = 256)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "manager_user_id", length = 128)
    private String managerUserId;

    @Column(name = "primary_dept_id")
    private Long primaryDeptId;

    @Column(name = "last_sync_at", nullable = false)
    private Instant lastSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrgUserProfile() {}

    public OrgUserProfile(String userId, String unionId, String name) {
        this.userId = userId;
        this.unionId = unionId;
        this.name = name;
        this.lastSyncAt = Instant.now(Clock.systemUTC());
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now(Clock.systemUTC());
        this.createdAt = now;
        this.updatedAt = now;
        if (this.lastSyncAt == null) {
            this.lastSyncAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now(Clock.systemUTC());
    }

    public String getUserId() { return userId; }
    public String getUnionId() { return unionId; }
    public void setUnionId(String unionId) { this.unionId = unionId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getManagerUserId() { return managerUserId; }
    public void setManagerUserId(String managerUserId) { this.managerUserId = managerUserId; }
    public Long getPrimaryDeptId() { return primaryDeptId; }
    public void setPrimaryDeptId(Long primaryDeptId) { this.primaryDeptId = primaryDeptId; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }
}
