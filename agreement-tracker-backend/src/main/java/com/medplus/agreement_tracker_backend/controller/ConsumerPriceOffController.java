package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.BulkPriceOffIdsRequest;
import com.medplus.agreement_tracker_backend.dto.request.BulkPriceOffRejectRequest;
import com.medplus.agreement_tracker_backend.dto.request.BulkUpdateCampaignIdRequest;
import com.medplus.agreement_tracker_backend.dto.request.PriceOffRejectRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpdateCampaignIdRequest;
import com.medplus.agreement_tracker_backend.dto.response.ConsumerPriceOffCampaignResponse;
import com.medplus.agreement_tracker_backend.dto.request.PriceOffListFilters;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.PriceOffUploadResultDto;
import com.medplus.agreement_tracker_backend.dto.response.PriceOffFilterOptionsResponse;
import com.medplus.agreement_tracker_backend.enums.PriceOffDiscountType;
import com.medplus.agreement_tracker_backend.enums.PriceOffDisplayStatus;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.ConsumerPriceOffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.PRICE_OFF_APPROVE;
import static com.medplus.agreement_tracker_backend.security.RightExpressions.PRICE_OFF_MANAGE;
import static com.medplus.agreement_tracker_backend.security.RightExpressions.PRICE_OFF_VIEW;

@RestController
@RequestMapping("/price-offs")
@RequiredArgsConstructor
public class ConsumerPriceOffController {

    private final ConsumerPriceOffService consumerPriceOffService;

    @GetMapping("/template")
    @PreAuthorize(PRICE_OFF_MANAGE)
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = consumerPriceOffService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=price-off-campaigns-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(PRICE_OFF_MANAGE)
    public ResponseEntity<PriceOffUploadResultDto> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consumerPriceOffService.uploadCampaigns(file, principal.getId()));
    }

    @GetMapping("/filter-options")
    @PreAuthorize(PRICE_OFF_VIEW)
    public ResponseEntity<PriceOffFilterOptionsResponse> filterOptions() {
        return ResponseEntity.ok(consumerPriceOffService.getFilterOptions());
    }

    @GetMapping
    @PreAuthorize(PRICE_OFF_VIEW)
    public ResponseEntity<PagedResponse<ConsumerPriceOffCampaignResponse>> list(
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String campaignId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) PriceOffDiscountType discountType,
            @RequestParam(required = false) PriceOffDisplayStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal UserPrincipal principal) {
        Sort sort = Sort.by("DESC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        PriceOffListFilters filters = new PriceOffListFilters(
                product, campaignId, location, channel, discountType, status);
        return ResponseEntity.ok(consumerPriceOffService.listCampaigns(
                filters, pageable, principal.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(PRICE_OFF_VIEW)
    public ResponseEntity<ConsumerPriceOffCampaignResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(consumerPriceOffService.getCampaign(id));
    }

    @PutMapping("/{id}/campaign-id")
    @PreAuthorize(PRICE_OFF_MANAGE)
    public ResponseEntity<ConsumerPriceOffCampaignResponse> updateCampaignId(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampaignIdRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consumerPriceOffService.updateCampaignId(id, request, principal.getId()));
    }

    @PutMapping("/bulk-campaign-id")
    @PreAuthorize(PRICE_OFF_MANAGE)
    public ResponseEntity<List<ConsumerPriceOffCampaignResponse>> bulkUpdateCampaignId(
            @Valid @RequestBody BulkUpdateCampaignIdRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consumerPriceOffService.bulkUpdateCampaignId(request, principal.getId()));
    }

    @PutMapping("/bulk-submit")
    @PreAuthorize(PRICE_OFF_MANAGE)
    public ResponseEntity<List<ConsumerPriceOffCampaignResponse>> bulkSubmit(
            @Valid @RequestBody BulkPriceOffIdsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consumerPriceOffService.bulkSubmit(request, principal.getId()));
    }

    @DeleteMapping("/bulk")
    @PreAuthorize(PRICE_OFF_MANAGE)
    public ResponseEntity<Void> bulkDelete(
            @Valid @RequestBody BulkPriceOffIdsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        consumerPriceOffService.bulkDelete(request, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/bulk-approve")
    @PreAuthorize(PRICE_OFF_APPROVE)
    public ResponseEntity<List<ConsumerPriceOffCampaignResponse>> bulkApprove(
            @Valid @RequestBody BulkPriceOffIdsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consumerPriceOffService.bulkApprove(request, principal.getId()));
    }

    @PutMapping("/bulk-reject")
    @PreAuthorize(PRICE_OFF_APPROVE)
    public ResponseEntity<List<ConsumerPriceOffCampaignResponse>> bulkReject(
            @Valid @RequestBody BulkPriceOffRejectRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consumerPriceOffService.bulkReject(request, principal.getId()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize(PRICE_OFF_APPROVE)
    public ResponseEntity<ConsumerPriceOffCampaignResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consumerPriceOffService.approve(id, principal.getId()));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize(PRICE_OFF_APPROVE)
    public ResponseEntity<ConsumerPriceOffCampaignResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody PriceOffRejectRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consumerPriceOffService.reject(
                id, request.remarks(), principal.getId()));
    }
}
