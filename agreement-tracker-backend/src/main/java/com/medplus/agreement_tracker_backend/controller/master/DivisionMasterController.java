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

import static com.medplus.agreement_tracker_backend.security.RightExpressions.*;

import java.util.List;

@RestController
@RequestMapping("/master")
@RequiredArgsConstructor
public class DivisionMasterController {

    private final DivisionMasterService service;

    @PostMapping("/divisions/search")
    @PreAuthorize(MASTER_VIEW)
    public ResponseEntity<PagedResponse<DivisionMasterResponse>> search(@RequestBody MasterPageRequest req) {
        return ResponseEntity.ok(service.search(req));
    }

    /** Cascade dropdown used by the agreement wizard. */
    @GetMapping("/manufacturers/{manufacturerId}/divisions")
    @PreAuthorize(MASTER_OR_AGREEMENT_READ)
    public ResponseEntity<List<DivisionMaster>> getByManufacturer(@PathVariable Long manufacturerId) {
        return ResponseEntity.ok(service.findByManufacturer(manufacturerId));
    }

    @GetMapping("/divisions")
    @PreAuthorize(MASTER_VIEW)
    public ResponseEntity<List<DivisionMaster>> listAll() {
        return ResponseEntity.ok(service.findAllActive());
    }

    @GetMapping("/divisions/{id}")
    @PreAuthorize(MASTER_VIEW)
    public ResponseEntity<DivisionMasterResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/divisions")
    @PreAuthorize(MASTER_MANAGE)
    public ResponseEntity<DivisionMasterResponse> create(
            @Valid @RequestBody DivisionMasterRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, principal.getId()));
    }

    @PutMapping("/divisions/{id}")
    @PreAuthorize(MASTER_MANAGE)
    public ResponseEntity<DivisionMasterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DivisionMasterRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.update(id, req, principal.getId()));
    }

    @PatchMapping("/divisions/{id}/toggle-status")
    @PreAuthorize(MASTER_MANAGE)
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.toggleStatus(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
