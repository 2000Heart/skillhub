package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.orgsync.OrgRole;
import com.iflytek.skillhub.domain.orgsync.OrgRoleRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgRoleJpaRepository extends JpaRepository<OrgRole, Long>, OrgRoleRepository {
}
