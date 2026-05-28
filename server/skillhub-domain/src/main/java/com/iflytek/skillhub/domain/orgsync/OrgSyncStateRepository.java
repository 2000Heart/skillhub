package com.iflytek.skillhub.domain.orgsync;

import java.util.Optional;

public interface OrgSyncStateRepository {
    OrgSyncState save(OrgSyncState state);
    Optional<OrgSyncState> findById(String syncKey);
}
