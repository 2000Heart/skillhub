package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.orgsync.OrgUserRole;
import com.iflytek.skillhub.domain.orgsync.OrgUserRoleRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgUserRoleJpaRepository extends JpaRepository<OrgUserRole, Long>, OrgUserRoleRepository {
    void deleteByUserId(String userId);
    List<OrgUserRole> findByUserId(String userId);
}
