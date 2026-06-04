package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.UserCompanyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCompanyAssignmentRepository extends JpaRepository<UserCompanyAssignment, Long> {

    List<UserCompanyAssignment> findByUserId(Long userId);

    List<UserCompanyAssignment> findByCompanyId(Long companyId);

    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    void deleteByUserIdAndCompanyId(Long userId, Long companyId);

    @Query("SELECT uca.company.id FROM UserCompanyAssignment uca WHERE uca.user.id = :userId")
    List<Long> findCompanyIdsByUserId(@Param("userId") Long userId);
}
