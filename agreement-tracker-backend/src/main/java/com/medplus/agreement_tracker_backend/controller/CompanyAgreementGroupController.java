package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.CreateCompanyAgreementGroupRequest;
import com.medplus.agreement_tracker_backend.dto.response.CompanyAgreementGroupResponse;
import com.medplus.agreement_tracker_backend.enums.RightCode;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.CompanyAgreementGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_CREATE;
import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_VIEW;

@RestController
@RequestMapping("/companies/{companyId}/agreement-groups")
@RequiredArgsConstructor
public class CompanyAgreementGroupController {

    private final CompanyAgreementGroupService companyAgreementGroupService;

    @GetMapping
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<List<CompanyAgreementGroupResponse>> listByCompany(
            @PathVariable Long companyId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(companyAgreementGroupService.listByCompanyId(
                companyId,
                principal.getId(),
                principal.hasRight(RightCode.AGREEMENT_VIEW_ALL.name())));
    }

    @PostMapping
    @PreAuthorize(AGREEMENT_CREATE)
    public ResponseEntity<CompanyAgreementGroupResponse> create(
            @PathVariable Long companyId,
            @Valid @RequestBody CreateCompanyAgreementGroupRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyAgreementGroupService.resolveOrCreate(
                        companyId, null, request.name(), principal.getId()));
    }
}
