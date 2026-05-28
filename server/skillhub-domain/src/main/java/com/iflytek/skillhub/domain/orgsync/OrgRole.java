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
@Table(name = "org_role")
public class OrgRole {
    @Id
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_name", length = 128)
    private String groupName;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "last_sync_at", nullable = false)
    private Instant lastSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrgRole() {}

    public OrgRole(Long roleId, Long groupId, String groupName, String name) {
        this.roleId = roleId;
        this.groupId = groupId;
        this.groupName = groupName;
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

    public Long getRoleId() { return roleId; }
    public String getName() { return name; }
    public Long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
}
