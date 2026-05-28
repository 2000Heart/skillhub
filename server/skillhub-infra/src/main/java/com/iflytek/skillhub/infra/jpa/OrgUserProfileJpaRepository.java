package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.orgsync.OrgUserProfile;
import com.iflytek.skillhub.domain.orgsync.OrgUserProfileRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgUserProfileJpaRepository extends JpaRepository<OrgUserProfile, String>, OrgUserProfileRepository {

    @Override
    java.util.Optional<OrgUserProfile> findByUnionId(String unionId);
}
