package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.orgsync.OrgEventCheckpoint;
import com.iflytek.skillhub.domain.orgsync.OrgEventCheckpointRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgEventCheckpointJpaRepository
        extends JpaRepository<OrgEventCheckpoint, String>, OrgEventCheckpointRepository {
}
