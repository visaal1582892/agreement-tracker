package com.medplus.agreement_tracker_backend.service.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.RightRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.RightResponse;
import com.medplus.agreement_tracker_backend.entity.Right;
import com.medplus.agreement_tracker_backend.exception.DuplicateResourceException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.RightRepository;
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
public class RightService {

    private final RightRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<RightResponse> search(MasterPageRequest req) {
        Specification<Right> spec = buildSpec(req.getFilters());
        Sort sort = Sort.by(Sort.Direction.fromString(req.getSortDirection()), req.getSortBy());
        Page<Right> page = repository.findAll(spec, PageRequest.of(req.getPage(), req.getSize(), sort));
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public RightResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<Right> findAllActive() {
        return repository.findByIsActiveTrue();
    }

    public RightResponse create(RightRequest req, Long userId) {
        if (repository.findByCode(req.getCode()).isPresent()) {
            throw new DuplicateResourceException("Right code already exists: " + req.getCode());
        }
        Right entity = Right.builder().code(req.getCode()).name(req.getName())
                .module(req.getModule()).description(req.getDescription()).build();
        entity.setCreatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public RightResponse update(Long id, RightRequest req, Long userId) {
        Right entity = findOrThrow(id);
        if (!entity.getCode().equalsIgnoreCase(req.getCode()) && repository.findByCode(req.getCode()).isPresent()) {
            throw new DuplicateResourceException("Right code already exists: " + req.getCode());
        }
        entity.setCode(req.getCode());
        entity.setName(req.getName());
        entity.setModule(req.getModule());
        entity.setDescription(req.getDescription());
        if (req.getIsActive() != null) entity.setActive(req.getIsActive());
        entity.setUpdatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public void toggleStatus(Long id, Long userId) {
        Right entity = findOrThrow(id);
        entity.setActive(!entity.isActive());
        entity.setUpdatedByUserId(userId);
        repository.save(entity);
    }

    private Right findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Right", id));
    }

    private Specification<Right> buildSpec(Map<String, String> filters) {
        Specification<Right> spec = SpecificationUtils.empty();
        if (filters == null || filters.isEmpty()) return spec;
        if (filters.containsKey("code")) spec = spec.and(SpecificationUtils.stringLike("code", filters.get("code")));
        if (filters.containsKey("name")) spec = spec.and(SpecificationUtils.stringLike("name", filters.get("name")));
        if (filters.containsKey("module")) spec = spec.and(SpecificationUtils.stringLike("module", filters.get("module")));
        if (filters.containsKey("isActive")) spec = spec.and(SpecificationUtils.booleanEquals("isActive", SpecificationUtils.parseBoolean(filters.get("isActive"))));
        return spec;
    }

    private RightResponse toResponse(Right e) {
        return RightResponse.builder()
                .id(e.getId()).code(e.getCode()).name(e.getName()).module(e.getModule())
                .description(e.getDescription()).isActive(e.isActive())
                .createdAt(e.getCreatedAt()).createdByUserId(e.getCreatedByUserId())
                .updatedAt(e.getUpdatedAt()).updatedByUserId(e.getUpdatedByUserId()).build();
    }
}
