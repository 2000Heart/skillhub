package com.iflytek.skillhub.domain.user;

import jakarta.persistence.*;
import java.time.Clock;
import java.time.Instant;

@Entity
@Table(name = "user_account")
public class UserAccount {
    @Id
    @Column(length = 128)
    private String id;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(length = 256)
    private String email;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "department", length = 128)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_source", length = 64)
    private PermissionSource permissionSource;

    @Column(name = "permission_initialized_at")
    private Instant permissionInitializedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "merged_to_user_id")
    private String mergedToUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() {}

    public UserAccount(String id, String displayName, String email, String avatarUrl) {
        this(id, displayName, email, avatarUrl, null);
    }

    public UserAccount(String id, String displayName, String email, String avatarUrl, String department) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.department = department;
        this.status = UserStatus.ACTIVE;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now(Clock.systemUTC());
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now(Clock.systemUTC());
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public PermissionSource getPermissionSource() { return permissionSource; }
    public void setPermissionSource(PermissionSource permissionSource) { this.permissionSource = permissionSource; }
    public Instant getPermissionInitializedAt() { return permissionInitializedAt; }
    public void setPermissionInitializedAt(Instant permissionInitializedAt) { this.permissionInitializedAt = permissionInitializedAt; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public String getMergedToUserId() { return mergedToUserId; }
    public void setMergedToUserId(String mergedToUserId) { this.mergedToUserId = mergedToUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean isActive() { return this.status == UserStatus.ACTIVE; }
}
