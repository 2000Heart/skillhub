package com.iflytek.skillhub.domain.orgsync;

import java.util.List;
import java.util.Optional;

public interface OrgDepartmentRepository {
    OrgDepartment save(OrgDepartment department);
    Optional<OrgDepartment> findById(Long deptId);
    List<OrgDepartment> findAll();
}
