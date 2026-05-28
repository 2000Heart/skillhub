package com.iflytek.skillhub.domain.orgsync;

import java.util.List;

public interface OrgUserDepartmentRepository {
    OrgUserDepartment save(OrgUserDepartment relation);
    void deleteByUserId(String userId);
    List<OrgUserDepartment> findByUserId(String userId);
}
