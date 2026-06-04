package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.UserRole;
import com.medplus.agreement_tracker_backend.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.user.id = :userId")
    List<UserRole> findByUserIdWithRole(@Param("userId") Long userId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.role.name = :roleName AND ur.user.isActive = true")
    List<UserRole> findActiveUsersByRole(@Param("roleName") RoleName roleName);
}
