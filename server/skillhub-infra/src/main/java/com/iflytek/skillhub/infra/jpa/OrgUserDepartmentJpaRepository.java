package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.orgsync.OrgUserDepartment;
import com.iflytek.skillhub.domain.orgsync.OrgUserDepartmentRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgUserDepartmentJpaRepository
        extends JpaRepository<OrgUserDepartment, Long>, OrgUserDepartmentRepository {
    void deleteByUserId(String userId);
    List<OrgUserDepartment> findByUserId(String userId);
}
