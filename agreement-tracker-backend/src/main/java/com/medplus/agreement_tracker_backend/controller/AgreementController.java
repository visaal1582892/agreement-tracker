package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.ApprovalActionRequest;
import com.medplus.agreement_tracker_backend.dto.request.BulkTransferRequest;
import com.medplus.agreement_tracker_backend.dto.request.CreateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.TerminateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementGroupResponse;
import com.medplus.agreement_tracker_backend.dto.response.AgreementResponse;
import com.medplus.agreement_tracker_backend.dto.response.ApprovalTimelineResponse;
import com.medplus.agreement_tracker_backend.enums.RoleName;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.AgreementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agreements")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNT_MANAGER')")
    public ResponseEntity<AgreementResponse> createDraft(
            @Valid @RequestBody CreateAgreementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agreementService.createDraft(request, principal.getId()));
    }

    @PostMapping("/groups/{groupId}/new-version")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNT_MANAGER')")
    public ResponseEntity<AgreementResponse> createNewVersion(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agreementService.createNewVersion(groupId, principal.getId()));
    }

    @GetMapping("/groups")
    public ResponseEntity<Page<AgreementGroupResponse>> getAllGroups(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + RoleName.ADMIN.name())
                        || a.getAuthority().equals("ROLE_" + RoleName.LEADERSHIP.name())
                        || a.getAuthority().equals("ROLE_" + RoleName.FINANCE.name()));
        return ResponseEntity.ok(agreementService.getAllGroups(pageable, principal.getId(), isAdmin));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<AgreementGroupResponse> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(agreementService.getGroupById(groupId));
    }

    @GetMapping("/groups/{groupId}/versions")
    public ResponseEntity<List<AgreementResponse>> getVersions(@PathVariable Long groupId) {
        return ResponseEntity.ok(agreementService.getVersionsByGroup(groupId));
    }

    @GetMapping("/{agreementId}")
    public ResponseEntity<AgreementResponse> getAgreement(@PathVariable Long agreementId) {
        return ResponseEntity.ok(agreementService.getAgreementById(agreementId));
    }

    @PostMapping("/{agreementId}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNT_MANAGER')")
    public ResponseEntity<AgreementResponse> submitForApproval(
            @PathVariable Long agreementId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agreementService.submitForApproval(agreementId, principal.getId()));
    }

    @PostMapping("/{agreementId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public ResponseEntity<AgreementResponse> approve(
            @PathVariable Long agreementId,
            @RequestBody(required = false) ApprovalActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String remarks = request != null ? request.remarks() : null;
        return ResponseEntity.ok(agreementService.approve(agreementId, remarks, principal.getId()));
    }

    @PostMapping("/{agreementId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public ResponseEntity<AgreementResponse> reject(
            @PathVariable Long agreementId,
            @Valid @RequestBody ApprovalActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agreementService.reject(agreementId, request.remarks(), principal.getId()));
    }

    @PostMapping("/{agreementId}/terminate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNT_MANAGER')")
    public ResponseEntity<AgreementResponse> terminate(
            @PathVariable Long agreementId,
            @Valid @RequestBody TerminateAgreementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agreementService.terminate(agreementId, request, principal.getId()));
    }

    @PatchMapping("/{agreementId}/in-progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNT_MANAGER')")
    public ResponseEntity<AgreementResponse> toggleInProgress(
            @PathVariable Long agreementId,
            @RequestParam boolean value,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agreementService.toggleInProgress(agreementId, value, principal.getId()));
    }

    @GetMapping("/pending-approvals")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    public ResponseEntity<Page<AgreementResponse>> getPendingApprovals(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(agreementService.getPendingApprovals(pageable));
    }

    @GetMapping("/{agreementId}/timeline")
    public ResponseEntity<List<ApprovalTimelineResponse>> getTimeline(@PathVariable Long agreementId) {
        return ResponseEntity.ok(agreementService.getApprovalTimeline(agreementId));
    }

    @PostMapping("/bulk-transfer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bulkTransfer(
            @Valid @RequestBody BulkTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        agreementService.bulkTransferOwnership(
                request.fromUserId(), request.toUserId(),
                request.specificAgreementGroupIds(), principal.getId());
        return ResponseEntity.ok().build();
    }
}
