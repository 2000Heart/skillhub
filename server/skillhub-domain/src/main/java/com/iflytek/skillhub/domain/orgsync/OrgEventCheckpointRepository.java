package com.iflytek.skillhub.domain.orgsync;

import java.util.Optional;

public interface OrgEventCheckpointRepository {
    OrgEventCheckpoint save(OrgEventCheckpoint checkpoint);
    Optional<OrgEventCheckpoint> findById(String eventKey);
}
