package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.CommercialTemplateRequest;
import com.medplus.agreement_tracker_backend.dto.request.CommercialTypeSwitchRequest;
import com.medplus.agreement_tracker_backend.dto.response.CommercialUploadResponse;
import com.medplus.agreement_tracker_backend.dto.request.UpsertTargetRequest;
import com.medplus.agreement_tracker_backend.dto.response.TimePeriodTargetsPreviewResponse;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.CommercialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_EDIT;
import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_VIEW;

@RestController
@RequestMapping("/agreement-versions/{agreementVersionId}/commercials")
@RequiredArgsConstructor
public class CommercialController {

    private final CommercialService commercialService;

    @PostMapping("/template")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<byte[]> generateTemplate(
            @PathVariable Long agreementVersionId,
            @Valid @RequestBody CommercialTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        byte[] file = commercialService.generateCommercialTemplate(agreementVersionId, request, principal.getId());

        String filename = "commercial-template-" + agreementVersionId + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<CommercialUploadResponse> uploadTargets(
            @PathVariable Long agreementVersionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) CommercialSlabType slabType,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                commercialService.uploadCommercialTargets(agreementVersionId, file, slabType, principal.getId()));
    }

    @GetMapping("/targets/preview")
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<List<TimePeriodTargetsPreviewResponse>> getTargetsPreview(
            @PathVariable Long agreementVersionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(commercialService.getTargetsPreview(agreementVersionId, principal.getId()));
    }

    @PutMapping("/targets")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<Void> upsertTarget(
            @PathVariable Long agreementVersionId,
            @Valid @RequestBody UpsertTargetRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        commercialService.upsertTarget(agreementVersionId, request, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/type-switch")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<Void> switchCommercialType(
            @PathVariable Long agreementVersionId,
            @Valid @RequestBody CommercialTypeSwitchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        commercialService.switchCommercialType(agreementVersionId, request, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
