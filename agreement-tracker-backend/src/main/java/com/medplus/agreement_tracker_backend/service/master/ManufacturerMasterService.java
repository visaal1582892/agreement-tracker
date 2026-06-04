package com.medplus.agreement_tracker_backend.service.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.ManufacturerMasterRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.ManufacturerMasterResponse;
import com.medplus.agreement_tracker_backend.entity.ManufacturerMaster;
import com.medplus.agreement_tracker_backend.exception.DuplicateResourceException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.ManufacturerMasterRepository;
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
public class ManufacturerMasterService {

    private final ManufacturerMasterRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<ManufacturerMasterResponse> search(MasterPageRequest req) {
        Specification<ManufacturerMaster> spec = buildSpec(req.getFilters());
        Sort sort = Sort.by(Sort.Direction.fromString(req.getSortDirection()), req.getSortBy());
        Page<ManufacturerMaster> page = repository.findAll(spec, PageRequest.of(req.getPage(), req.getSize(), sort));
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ManufacturerMasterResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ManufacturerMaster> findAllActive() {
        return repository.findByIsActiveTrue();
    }

    public ManufacturerMasterResponse create(ManufacturerMasterRequest req, Long userId) {
        ManufacturerMaster entity = ManufacturerMaster.builder()
                .manufacturerCode(req.getManufacturerCode())
                .manufacturerName(req.getManufacturerName())
                .build();
        entity.setCreatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public ManufacturerMasterResponse update(Long id, ManufacturerMasterRequest req, Long userId) {
        ManufacturerMaster entity = findOrThrow(id);
        entity.setManufacturerCode(req.getManufacturerCode());
        entity.setManufacturerName(req.getManufacturerName());
        if (req.getIsActive() != null) entity.setActive(req.getIsActive());
        entity.setUpdatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public void toggleStatus(Long id, Long userId) {
        ManufacturerMaster entity = findOrThrow(id);
        entity.setActive(!entity.isActive());
        entity.setUpdatedByUserId(userId);
        repository.save(entity);
    }

    private ManufacturerMaster findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Manufacturer", id));
    }

    private Specification<ManufacturerMaster> buildSpec(Map<String, String> filters) {
        Specification<ManufacturerMaster> spec = SpecificationUtils.empty();
        if (filters == null || filters.isEmpty()) return spec;
        if (filters.containsKey("manufacturerName")) {
            spec = spec.and(SpecificationUtils.stringLike("manufacturerName", filters.get("manufacturerName")));
        }
        if (filters.containsKey("manufacturerCode")) {
            spec = spec.and(SpecificationUtils.stringLike("manufacturerCode", filters.get("manufacturerCode")));
        }
        if (filters.containsKey("isActive")) {
            spec = spec.and(SpecificationUtils.booleanEquals("isActive",
                    SpecificationUtils.parseBoolean(filters.get("isActive"))));
        }
        return spec;
    }

    private ManufacturerMasterResponse toResponse(ManufacturerMaster e) {
        return ManufacturerMasterResponse.builder()
                .id(e.getId())
                .manufacturerCode(e.getManufacturerCode())
                .manufacturerName(e.getManufacturerName())
                .isActive(e.isActive())
                .createdAt(e.getCreatedAt())
                .createdByUserId(e.getCreatedByUserId())
                .updatedAt(e.getUpdatedAt())
                .updatedByUserId(e.getUpdatedByUserId())
                .build();
    }
}
