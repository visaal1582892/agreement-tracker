package com.medplus.agreement_tracker_backend.controller.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.DivisionMasterRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.DivisionMasterResponse;
import com.medplus.agreement_tracker_backend.entity.DivisionMaster;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.master.DivisionMasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/master")
@RequiredArgsConstructor
public class DivisionMasterController {

    private final DivisionMasterService service;

    @PostMapping("/divisions/search")
    public ResponseEntity<PagedResponse<DivisionMasterResponse>> search(@RequestBody MasterPageRequest req) {
        return ResponseEntity.ok(service.search(req));
    }

    /** Cascade dropdown used by the agreement wizard. */
    @GetMapping("/manufacturers/{manufacturerId}/divisions")
    public ResponseEntity<List<DivisionMaster>> getByManufacturer(@PathVariable Long manufacturerId) {
        return ResponseEntity.ok(service.findByManufacturer(manufacturerId));
    }

    @GetMapping("/divisions")
    public ResponseEntity<List<DivisionMaster>> listAll() {
        return ResponseEntity.ok(service.findAllActive());
    }

    @GetMapping("/divisions/{id}")
    public ResponseEntity<DivisionMasterResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/divisions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DivisionMasterResponse> create(
            @Valid @RequestBody DivisionMasterRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, principal.getId()));
    }

    @PutMapping("/divisions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DivisionMasterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DivisionMasterRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.update(id, req, principal.getId()));
    }

    @PatchMapping("/divisions/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.toggleStatus(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
