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
@Table(name = "org_user_department", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "dept_id"}))
public class OrgUserDepartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryDepartment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrgUserDepartment() {}

    public OrgUserDepartment(String userId, Long deptId, boolean primaryDepartment) {
        this.userId = userId;
        this.deptId = deptId;
        this.primaryDepartment = primaryDepartment;
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
    public Long getDeptId() { return deptId; }
    public boolean isPrimaryDepartment() { return primaryDepartment; }
}
