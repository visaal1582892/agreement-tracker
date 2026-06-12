package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.GroupDeletionRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpdateCompanyAgreementGroupRequest;
import com.medplus.agreement_tracker_backend.dto.response.BulkGroupSubmitResponse;
import com.medplus.agreement_tracker_backend.dto.response.CompanyAgreementGroupResponse;
import com.medplus.agreement_tracker_backend.dto.response.GroupDeletionStatusResponse;
import com.medplus.agreement_tracker_backend.enums.RightCode;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.AgreementService;
import com.medplus.agreement_tracker_backend.service.CompanyAgreementGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.*;

@RestController
@RequestMapping("/company-agreement-groups")
@RequiredArgsConstructor
public class CompanyAgreementGroupResourceController {

    private final CompanyAgreementGroupService companyAgreementGroupService;
    private final AgreementService agreementService;

    @GetMapping
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<Page<CompanyAgreementGroupResponse>> listAll(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String lastModifiedBy,
            @RequestParam(required = false) String createdBy,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(companyAgreementGroupService.listAll(
                pageable, companyId, isActive, groupName, lastModifiedBy, createdBy,
                principal.getId(), principal.hasRight(RightCode.AGREEMENT_VIEW_ALL.name()),
                principal.hasRole("APPROVER"), principal.hasRole("ACCOUNT_MANAGER")));
    }

    @GetMapping("/{groupId}")
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<CompanyAgreementGroupResponse> getById(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(companyAgreementGroupService.getById(
                groupId, principal.getId(), principal.hasRight(RightCode.AGREEMENT_VIEW_ALL.name()),
                principal.hasRole("APPROVER"), principal.hasRole("ACCOUNT_MANAGER")));
    }

    @GetMapping("/{groupId}/deletion-status")
    @PreAuthorize("hasAnyAuthority('AGREEMENT_CREATE', 'AGREEMENT_APPROVE')")
    public ResponseEntity<GroupDeletionStatusResponse> getDeletionStatus(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(companyAgreementGroupService.getDeletionStatus(
                groupId,
                principal.getId(),
                principal.hasRole("APPROVER"),
                principal.hasRole("ACCOUNT_MANAGER")));
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasAnyAuthority('AGREEMENT_CREATE', 'AGREEMENT_APPROVE')")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            @RequestParam String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        companyAgreementGroupService.deleteGroupImmediately(
                groupId,
                reason,
                principal.getId(),
                principal.hasRole("APPROVER"),
                principal.hasRole("ACCOUNT_MANAGER"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/deletion-requests")
    @PreAuthorize(AGREEMENT_CREATE)
    public ResponseEntity<Void> submitDeletionRequest(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupDeletionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        companyAgreementGroupService.submitDeletionRequest(
                groupId, request, principal.getId(),
                principal.hasRole("APPROVER"), principal.hasRole("ACCOUNT_MANAGER"));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasAuthority('ADMIN_USERS')")
    public ResponseEntity<CompanyAgreementGroupResponse> renameGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateCompanyAgreementGroupRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                companyAgreementGroupService.renameGroup(groupId, request, principal.getId()));
    }

    @PostMapping("/{groupId}/submit-for-approval")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<BulkGroupSubmitResponse> submitGroupForApproval(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(agreementService.submitGroupDraftsForApproval(groupId, principal.getId()));
    }
}
