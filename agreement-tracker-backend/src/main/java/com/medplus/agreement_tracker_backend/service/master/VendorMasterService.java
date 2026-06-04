package com.medplus.agreement_tracker_backend.service.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.VendorMasterRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.VendorMasterResponse;
import com.medplus.agreement_tracker_backend.entity.VendorMaster;
import com.medplus.agreement_tracker_backend.exception.DuplicateResourceException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.VendorMasterRepository;
import com.medplus.agreement_tracker_backend.util.SpecificationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class VendorMasterService {

    private final VendorMasterRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<VendorMasterResponse> search(MasterPageRequest req) {
        Specification<VendorMaster> spec = buildSpec(req.getFilters());
        Sort sort = Sort.by(Sort.Direction.fromString(req.getSortDirection()), req.getSortBy());
        Page<VendorMaster> page = repository.findAll(spec, PageRequest.of(req.getPage(), req.getSize(), sort));
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public VendorMasterResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<VendorMaster> findAllActive() {
        return repository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<VendorMaster> searchVendors(String search) {
        return repository.searchVendors(search);
    }

    public VendorMasterResponse create(VendorMasterRequest req, Long userId) {
        if (req.getVendorCode() != null && !req.getVendorCode().isBlank()
                && repository.findByVendorCode(req.getVendorCode()).isPresent()) {
            throw new DuplicateResourceException("Vendor code already exists: " + req.getVendorCode());
        }
        VendorMaster entity = VendorMaster.builder()
                .vendorCode(req.getVendorCode())
                .vendorName(req.getVendorName())
                .build();
        entity.setCreatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public VendorMasterResponse update(Long id, VendorMasterRequest req, Long userId) {
        VendorMaster entity = findOrThrow(id);
        if (req.getVendorCode() != null && !req.getVendorCode().isBlank()
                && !req.getVendorCode().equalsIgnoreCase(entity.getVendorCode())
                && repository.findByVendorCode(req.getVendorCode()).isPresent()) {
            throw new DuplicateResourceException("Vendor code already exists: " + req.getVendorCode());
        }
        entity.setVendorCode(req.getVendorCode());
        entity.setVendorName(req.getVendorName());
        if (req.getIsActive() != null) entity.setActive(req.getIsActive());
        entity.setUpdatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public void toggleStatus(Long id, Long userId) {
        VendorMaster entity = findOrThrow(id);
        entity.setActive(!entity.isActive());
        entity.setUpdatedByUserId(userId);
        repository.save(entity);
    }

    private VendorMaster findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vendor", id));
    }

    private Specification<VendorMaster> buildSpec(Map<String, String> filters) {
        Specification<VendorMaster> spec = SpecificationUtils.empty();
        if (filters == null || filters.isEmpty()) return spec;
        if (filters.containsKey("vendorName")) {
            spec = spec.and(SpecificationUtils.stringLike("vendorName", filters.get("vendorName")));
        }
        if (filters.containsKey("vendorCode")) {
            spec = spec.and(SpecificationUtils.stringLike("vendorCode", filters.get("vendorCode")));
        }
        if (filters.containsKey("isActive")) {
            spec = spec.and(SpecificationUtils.booleanEquals("isActive",
                    SpecificationUtils.parseBoolean(filters.get("isActive"))));
        }
        return spec;
    }

    private VendorMasterResponse toResponse(VendorMaster e) {
        return VendorMasterResponse.builder()
                .id(e.getId())
                .vendorCode(e.getVendorCode())
                .vendorName(e.getVendorName())
                .isActive(e.isActive())
                .createdAt(e.getCreatedAt())
                .createdByUserId(e.getCreatedByUserId())
                .updatedAt(e.getUpdatedAt())
                .updatedByUserId(e.getUpdatedByUserId())
                .build();
    }
}
