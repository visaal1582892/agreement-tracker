package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.RoleRight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRightRepository extends JpaRepository<RoleRight, Long> {

    List<RoleRight> findByRoleId(Long roleId);

    @Query("SELECT rr.right.code FROM RoleRight rr WHERE rr.role.id IN :roleIds AND rr.right.isActive = true")
    List<String> findRightCodesByRoleIds(@Param("roleIds") List<Long> roleIds);
}
