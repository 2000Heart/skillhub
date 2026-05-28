package com.iflytek.skillhub.domain.orgsync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Clock;
import java.time.Instant;

@Entity
@Table(name = "org_user_role", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"}))
public class OrgUserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "role_name", nullable = false, length = 128)
    private String roleName;

    @Column(name = "source_scope", length = 128)
    private String sourceScope;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrgUserRole() {}

    public OrgUserRole(String userId, Long roleId, String roleName, String sourceScope) {
        this.userId = userId;
        this.roleId = roleId;
        this.roleName = roleName;
        this.sourceScope = sourceScope;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now(Clock.systemUTC());
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now(Clock.systemUTC());
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Long getRoleId() { return roleId; }
    public String getRoleName() { return roleName; }
}
