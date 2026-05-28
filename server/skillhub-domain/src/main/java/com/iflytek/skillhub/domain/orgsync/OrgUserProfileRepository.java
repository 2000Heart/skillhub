package com.iflytek.skillhub.domain.orgsync;

import java.util.List;
import java.util.Optional;

public interface OrgUserProfileRepository {
    OrgUserProfile save(OrgUserProfile profile);
    Optional<OrgUserProfile> findById(String userId);
    Optional<OrgUserProfile> findByUnionId(String unionId);
    List<OrgUserProfile> findAll();
}
