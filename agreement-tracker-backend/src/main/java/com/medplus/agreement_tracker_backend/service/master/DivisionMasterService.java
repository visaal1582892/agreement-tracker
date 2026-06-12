package com.medplus.agreement_tracker_backend.service.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.DivisionMasterRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.DivisionMasterResponse;
import com.medplus.agreement_tracker_backend.entity.DivisionMaster;
import com.medplus.agreement_tracker_backend.entity.ManufacturerMaster;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.DivisionMasterRepository;
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
public class DivisionMasterService {

    private final DivisionMasterRepository divisionRepository;
    private final ManufacturerMasterRepository manufacturerRepository;

    @Transactional(readOnly = true)
    public PagedResponse<DivisionMasterResponse> search(MasterPageRequest req) {
        Specification<DivisionMaster> spec = buildSpec(req.getFilters());
        Sort sort = Sort.by(Sort.Direction.fromString(req.getSortDirection()), req.getSortBy());
        Page<DivisionMaster> page = divisionRepository.findAll(spec, PageRequest.of(req.getPage(), req.getSize(), sort));
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public DivisionMasterResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<DivisionMasterResponse> findByManufacturer(Long manufacturerId) {
        return divisionRepository.findActiveByManufacturerId(manufacturerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DivisionMasterResponse> findAllActive() {
        return divisionRepository.findAllActiveWithManufacturer().stream()
                .map(this::toResponse)
                .toList();
    }

    public DivisionMasterResponse create(DivisionMasterRequest req, Long userId) {
        ManufacturerMaster manufacturer = manufacturerRepository.findById(req.getManufacturerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", req.getManufacturerId()));
        DivisionMaster entity = DivisionMaster.builder()
                .divisionCode(req.getDivisionCode())
                .divisionName(req.getDivisionName())
                .manufacturer(manufacturer)
                .build();
        entity.setCreatedByUserId(userId);
        return toResponse(divisionRepository.save(entity));
    }

    public DivisionMasterResponse update(Long id, DivisionMasterRequest req, Long userId) {
        DivisionMaster entity = findOrThrow(id);
        ManufacturerMaster manufacturer = manufacturerRepository.findById(req.getManufacturerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", req.getManufacturerId()));
        entity.setDivisionCode(req.getDivisionCode());
        entity.setDivisionName(req.getDivisionName());
        entity.setManufacturer(manufacturer);
        if (req.getIsActive() != null) entity.setActive(req.getIsActive());
        entity.setUpdatedByUserId(userId);
        return toResponse(divisionRepository.save(entity));
    }

    public void toggleStatus(Long id, Long userId) {
        DivisionMaster entity = findOrThrow(id);
        entity.setActive(!entity.isActive());
        entity.setUpdatedByUserId(userId);
        divisionRepository.save(entity);
    }

    private DivisionMaster findOrThrow(Long id) {
        return divisionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Division", id));
    }

    private Specification<DivisionMaster> buildSpec(Map<String, String> filters) {
        Specification<DivisionMaster> spec = SpecificationUtils.empty();
        if (filters == null || filters.isEmpty()) return spec;
        if (filters.containsKey("divisionName")) {
            spec = spec.and(SpecificationUtils.stringLike("divisionName", filters.get("divisionName")));
        }
        if (filters.containsKey("divisionCode")) {
            spec = spec.and(SpecificationUtils.stringLike("divisionCode", filters.get("divisionCode")));
        }
        if (filters.containsKey("manufacturerId")) {
            spec = spec.and(SpecificationUtils.joinIdEquals("manufacturer",
                    SpecificationUtils.parseLong(filters.get("manufacturerId"))));
        }
        if (filters.containsKey("manufacturerName")) {
            spec = spec.and(SpecificationUtils.joinStringLike("manufacturer", "manufacturerName",
                    filters.get("manufacturerName")));
        }
        if (filters.containsKey("isActive")) {
            spec = spec.and(SpecificationUtils.booleanEquals("isActive",
                    SpecificationUtils.parseBoolean(filters.get("isActive"))));
        }
        return spec;
    }

    private DivisionMasterResponse toResponse(DivisionMaster e) {
        return DivisionMasterResponse.builder()
                .id(e.getId())
                .divisionCode(e.getDivisionCode())
                .divisionName(e.getDivisionName())
                .manufacturerId(e.getManufacturer() != null ? e.getManufacturer().getId() : null)
                .manufacturerName(e.getManufacturer() != null ? e.getManufacturer().getManufacturerName() : null)
                .isActive(e.isActive())
                .createdAt(e.getCreatedAt())
                .createdByUserId(e.getCreatedByUserId())
                .updatedAt(e.getUpdatedAt())
                .updatedByUserId(e.getUpdatedByUserId())
                .build();
    }
}
