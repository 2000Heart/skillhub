package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.orgsync.OrgDepartment;
import com.iflytek.skillhub.domain.orgsync.OrgDepartmentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgDepartmentJpaRepository extends JpaRepository<OrgDepartment, Long>, OrgDepartmentRepository {
}
