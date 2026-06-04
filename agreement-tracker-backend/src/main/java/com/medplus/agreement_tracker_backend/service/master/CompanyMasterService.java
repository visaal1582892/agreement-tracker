package com.medplus.agreement_tracker_backend.service.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.CompanyMasterRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.CompanyMasterResponse;
import com.medplus.agreement_tracker_backend.entity.CompanyMaster;
import com.medplus.agreement_tracker_backend.exception.DuplicateResourceException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.CompanyMasterRepository;
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
public class CompanyMasterService {

    private final CompanyMasterRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<CompanyMasterResponse> search(MasterPageRequest req) {
        Specification<CompanyMaster> spec = buildSpec(req.getFilters());
        Sort sort = Sort.by(Sort.Direction.fromString(req.getSortDirection()), req.getSortBy());
        Page<CompanyMaster> page = repository.findAll(spec, PageRequest.of(req.getPage(), req.getSize(), sort));
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public CompanyMasterResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CompanyMaster> findAllActive() {
        return repository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<CompanyMaster> searchByName(String search) {
        return repository.searchByName(search);
    }

    public CompanyMasterResponse create(CompanyMasterRequest req, Long userId) {
        if (repository.existsByCompanyNameIgnoreCase(req.getCompanyName())) {
            throw new DuplicateResourceException("Company already exists: " + req.getCompanyName());
        }
        CompanyMaster entity = CompanyMaster.builder().companyName(req.getCompanyName()).build();
        entity.setCreatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public CompanyMasterResponse update(Long id, CompanyMasterRequest req, Long userId) {
        CompanyMaster entity = findOrThrow(id);
        if (!entity.getCompanyName().equalsIgnoreCase(req.getCompanyName())
                && repository.existsByCompanyNameIgnoreCase(req.getCompanyName())) {
            throw new DuplicateResourceException("Company already exists: " + req.getCompanyName());
        }
        entity.setCompanyName(req.getCompanyName());
        if (req.getIsActive() != null) {
            entity.setActive(req.getIsActive());
        }
        entity.setUpdatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public void toggleStatus(Long id, Long userId) {
        CompanyMaster entity = findOrThrow(id);
        entity.setActive(!entity.isActive());
        entity.setUpdatedByUserId(userId);
        repository.save(entity);
    }

    private CompanyMaster findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    private Specification<CompanyMaster> buildSpec(Map<String, String> filters) {
        Specification<CompanyMaster> spec = SpecificationUtils.empty();
        if (filters == null || filters.isEmpty()) return spec;
        if (filters.containsKey("companyName")) {
            spec = spec.and(SpecificationUtils.stringLike("companyName", filters.get("companyName")));
        }
        if (filters.containsKey("isActive")) {
            spec = spec.and(SpecificationUtils.booleanEquals("isActive",
                    SpecificationUtils.parseBoolean(filters.get("isActive"))));
        }
        return spec;
    }

    private CompanyMasterResponse toResponse(CompanyMaster e) {
        return CompanyMasterResponse.builder()
                .id(e.getId())
                .companyName(e.getCompanyName())
                .isActive(e.isActive())
                .createdAt(e.getCreatedAt())
                .createdByUserId(e.getCreatedByUserId())
                .updatedAt(e.getUpdatedAt())
                .updatedByUserId(e.getUpdatedByUserId())
                .build();
    }
}
