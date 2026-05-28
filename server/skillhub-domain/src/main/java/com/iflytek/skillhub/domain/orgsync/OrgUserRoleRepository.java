package com.iflytek.skillhub.domain.orgsync;

import java.util.List;

public interface OrgUserRoleRepository {
    OrgUserRole save(OrgUserRole role);
    void deleteAll();
    void deleteByUserId(String userId);
    List<OrgUserRole> findByUserId(String userId);
}
