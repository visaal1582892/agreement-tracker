package com.medplus.agreement_tracker_backend.controller.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.ProductMasterRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.ProductMasterResponse;
import com.medplus.agreement_tracker_backend.entity.ProductMaster;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.master.ProductMasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/master/products")
@RequiredArgsConstructor
public class ProductMasterController {

    private final ProductMasterService service;

    @PostMapping("/search")
    public ResponseEntity<PagedResponse<ProductMasterResponse>> search(@RequestBody MasterPageRequest req) {
        return ResponseEntity.ok(service.search(req));
    }

    /** Wizard backward-compat endpoint. */
    @GetMapping
    public ResponseEntity<List<ProductMaster>> list(
            @RequestParam List<Long> vendorIds,
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(required = false) List<Long> divisionIds) {
        if (divisionIds != null && !divisionIds.isEmpty()) {
            return ResponseEntity.ok(service.findByVendorIdsAndDivisions(vendorIds, divisionIds));
        }
        if (manufacturerId != null) {
            return ResponseEntity.ok(service.findByVendorIdsAndManufacturer(vendorIds, manufacturerId));
        }
        return ResponseEntity.ok(service.findByVendorIds(vendorIds));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductMasterResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductMasterResponse> create(
            @Valid @RequestBody ProductMasterRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductMasterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductMasterRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.update(id, req, principal.getId()));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.toggleStatus(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
