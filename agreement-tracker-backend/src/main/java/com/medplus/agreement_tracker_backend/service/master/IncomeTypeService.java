package com.medplus.agreement_tracker_backend.service.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.IncomeTypeRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.IncomeTypeResponse;
import com.medplus.agreement_tracker_backend.entity.IncomeType;
import com.medplus.agreement_tracker_backend.exception.DuplicateResourceException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.IncomeTypeRepository;
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
public class IncomeTypeService {

    private final IncomeTypeRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<IncomeTypeResponse> search(MasterPageRequest req) {
        Specification<IncomeType> spec = buildSpec(req.getFilters());
        Sort sort = Sort.by(Sort.Direction.fromString(req.getSortDirection()), req.getSortBy());
        Page<IncomeType> page = repository.findAll(spec, PageRequest.of(req.getPage(), req.getSize(), sort));
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public IncomeTypeResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<IncomeType> findAllActive() {
        return repository.findByIsActiveTrue();
    }

    public IncomeTypeResponse create(IncomeTypeRequest req, Long userId) {
        if (repository.existsByNameIgnoreCase(req.getName())) {
            throw new DuplicateResourceException("Income type already exists: " + req.getName());
        }
        IncomeType entity = IncomeType.builder().name(req.getName()).description(req.getDescription()).build();
        entity.setCreatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public IncomeTypeResponse update(Long id, IncomeTypeRequest req, Long userId) {
        IncomeType entity = findOrThrow(id);
        if (!entity.getName().equalsIgnoreCase(req.getName()) && repository.existsByNameIgnoreCase(req.getName())) {
            throw new DuplicateResourceException("Income type already exists: " + req.getName());
        }
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        if (req.getIsActive() != null) entity.setActive(req.getIsActive());
        entity.setUpdatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public void toggleStatus(Long id, Long userId) {
        IncomeType entity = findOrThrow(id);
        entity.setActive(!entity.isActive());
        entity.setUpdatedByUserId(userId);
        repository.save(entity);
    }

    private IncomeType findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("IncomeType", id));
    }

    private Specification<IncomeType> buildSpec(Map<String, String> filters) {
        Specification<IncomeType> spec = SpecificationUtils.empty();
        if (filters == null || filters.isEmpty()) return spec;
        if (filters.containsKey("name")) spec = spec.and(SpecificationUtils.stringLike("name", filters.get("name")));
        if (filters.containsKey("description")) spec = spec.and(SpecificationUtils.stringLike("description", filters.get("description")));
        if (filters.containsKey("isActive")) spec = spec.and(SpecificationUtils.booleanEquals("isActive", SpecificationUtils.parseBoolean(filters.get("isActive"))));
        return spec;
    }

    private IncomeTypeResponse toResponse(IncomeType e) {
        return IncomeTypeResponse.builder()
                .id(e.getId()).name(e.getName()).description(e.getDescription()).isActive(e.isActive())
                .createdAt(e.getCreatedAt()).createdByUserId(e.getCreatedByUserId())
                .updatedAt(e.getUpdatedAt()).updatedByUserId(e.getUpdatedByUserId()).build();
    }
}
