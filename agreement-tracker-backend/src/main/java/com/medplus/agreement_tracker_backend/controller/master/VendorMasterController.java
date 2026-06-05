package com.medplus.agreement_tracker_backend.controller.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.VendorMasterRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.VendorMasterResponse;
import com.medplus.agreement_tracker_backend.entity.VendorMaster;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.master.VendorMasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.*;

import java.util.List;

@RestController
@RequestMapping("/master/vendors")
@RequiredArgsConstructor
public class VendorMasterController {

    private final VendorMasterService service;

    @PostMapping("/search")
    @PreAuthorize(MASTER_VIEW)
    public ResponseEntity<PagedResponse<VendorMasterResponse>> search(@RequestBody MasterPageRequest req) {
        return ResponseEntity.ok(service.search(req));
    }

    @GetMapping
    @PreAuthorize(MASTER_OR_AGREEMENT_READ)
    public ResponseEntity<List<VendorMaster>> list(@RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) return ResponseEntity.ok(service.searchVendors(search));
        return ResponseEntity.ok(service.findAllActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize(MASTER_VIEW)
    public ResponseEntity<VendorMasterResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize(MASTER_MANAGE)
    public ResponseEntity<VendorMasterResponse> create(
            @Valid @RequestBody VendorMasterRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(MASTER_MANAGE)
    public ResponseEntity<VendorMasterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VendorMasterRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.update(id, req, principal.getId()));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize(MASTER_MANAGE)
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.toggleStatus(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
