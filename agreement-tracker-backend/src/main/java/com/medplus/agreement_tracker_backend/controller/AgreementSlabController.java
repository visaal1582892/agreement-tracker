package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.dto.response.AgreementSlabResponse;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.AgreementSlabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_EDIT;
import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_VIEW;

@RestController
@RequestMapping("/agreement-versions/{agreementVersionId}/slabs")
@RequiredArgsConstructor
public class AgreementSlabController {

    private final AgreementSlabService slabService;

    @GetMapping
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<List<AgreementSlabResponse>> listSlabs(
            @PathVariable Long agreementVersionId,
            @RequestParam(required = false) CommercialSlabType slabType,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(slabService.listSlabs(agreementVersionId, slabType, principal.getId()));
    }

    @PostMapping
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<AgreementSlabResponse> createSlab(
            @PathVariable Long agreementVersionId,
            @Valid @RequestBody SlabDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(slabService.createSlab(agreementVersionId, request, principal.getId()));
    }

    @PutMapping("/{slabId}")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<AgreementSlabResponse> updateSlab(
            @PathVariable Long agreementVersionId,
            @PathVariable Long slabId,
            @Valid @RequestBody SlabDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                slabService.updateSlab(agreementVersionId, slabId, request, principal.getId()));
    }

    @DeleteMapping("/{slabId}")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<Void> deleteSlab(
            @PathVariable Long agreementVersionId,
            @PathVariable Long slabId,
            @AuthenticationPrincipal UserPrincipal principal) {
        slabService.deleteSlab(agreementVersionId, slabId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
