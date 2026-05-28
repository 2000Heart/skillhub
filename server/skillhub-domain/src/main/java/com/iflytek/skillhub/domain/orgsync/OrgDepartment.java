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
@Table(name = "org_department")
public class OrgDepartment {
    @Id
    @Column(name = "dept_id")
    private Long deptId;

    @Column(name = "parent_dept_id")
    private Long parentDeptId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "dept_order")
    private Long deptOrder;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "last_sync_at", nullable = false)
    private Instant lastSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrgDepartment() {}

    public OrgDepartment(Long deptId, Long parentDeptId, String name, Long deptOrder) {
        this.deptId = deptId;
        this.parentDeptId = parentDeptId;
        this.name = name;
        this.deptOrder = deptOrder;
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

    public Long getDeptId() { return deptId; }
    public Long getParentDeptId() { return parentDeptId; }
    public void setParentDeptId(Long parentDeptId) { this.parentDeptId = parentDeptId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getDeptOrder() { return deptOrder; }
    public void setDeptOrder(Long deptOrder) { this.deptOrder = deptOrder; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }
}
