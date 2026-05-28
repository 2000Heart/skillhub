package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.orgsync.OrgSyncState;
import com.iflytek.skillhub.domain.orgsync.OrgSyncStateRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgSyncStateJpaRepository extends JpaRepository<OrgSyncState, String>, OrgSyncStateRepository {
}
