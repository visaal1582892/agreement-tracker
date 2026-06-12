package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.GroupDeletionRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpdateCompanyAgreementGroupRequest;
import com.medplus.agreement_tracker_backend.dto.response.CompanyAgreementGroupResponse;
import com.medplus.agreement_tracker_backend.dto.response.GroupDeletionStatusResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CompanyAgreementGroupService {

    Page<CompanyAgreementGroupResponse> listAll(
            Pageable pageable, Long companyId, Boolean isActive, String groupName,
            String lastModifiedBy, String createdBy, Long currentUserId, boolean canViewAll,
            boolean isApprover, boolean isAccountManager);

    CompanyAgreementGroupResponse getById(Long groupId, Long currentUserId, boolean canViewAll,
                                          boolean isApprover, boolean isAccountManager);

    List<CompanyAgreementGroupResponse> listByCompanyId(Long companyId, Long currentUserId, boolean canViewAll);

    CompanyAgreementGroupResponse resolveOrCreate(Long companyId, Long groupId, String newName, Long userId);

    GroupDeletionStatusResponse getDeletionStatus(Long groupId, Long currentUserId,
                                                  boolean isApprover, boolean isAccountManager);

    void deleteGroupImmediately(Long groupId, String reason, Long currentUserId,
                                boolean isApprover, boolean isAccountManager);

    void submitDeletionRequest(Long groupId, GroupDeletionRequest request, Long currentUserId,
                               boolean isApprover, boolean isAccountManager);

    void executeApprovedGroupDeletion(Long groupId, Long approverId, String reason);

    CompanyAgreementGroupResponse renameGroup(Long groupId, UpdateCompanyAgreementGroupRequest request,
                                            Long currentUserId);
}
