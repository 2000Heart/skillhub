package com.iflytek.skillhub.domain.orgsync;

import java.util.List;
import java.util.Optional;

public interface OrgRoleRepository {
    OrgRole save(OrgRole role);
    Optional<OrgRole> findById(Long roleId);
    List<OrgRole> findAll();
}
