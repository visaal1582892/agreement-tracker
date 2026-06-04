package com.medplus.agreement_tracker_backend.service.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.AgreementTypeRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.AgreementTypeResponse;
import com.medplus.agreement_tracker_backend.entity.AgreementType;
import com.medplus.agreement_tracker_backend.exception.DuplicateResourceException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.AgreementTypeRepository;
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
public class AgreementTypeService {

    private final AgreementTypeRepository repository;

    @Transactional(readOnly = true)
    public PagedResponse<AgreementTypeResponse> search(MasterPageRequest req) {
        Specification<AgreementType> spec = buildSpec(req.getFilters());
        Sort sort = Sort.by(Sort.Direction.fromString(req.getSortDirection()), req.getSortBy());
        Page<AgreementType> page = repository.findAll(spec, PageRequest.of(req.getPage(), req.getSize(), sort));
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AgreementTypeResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AgreementType> findAllActive() {
        return repository.findByIsActiveTrue();
    }

    public AgreementTypeResponse create(AgreementTypeRequest req, Long userId) {
        if (repository.existsByNameIgnoreCase(req.getName())) {
            throw new DuplicateResourceException("Agreement type already exists: " + req.getName());
        }
        AgreementType entity = AgreementType.builder().name(req.getName()).build();
        entity.setCreatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public AgreementTypeResponse update(Long id, AgreementTypeRequest req, Long userId) {
        AgreementType entity = findOrThrow(id);
        if (!entity.getName().equalsIgnoreCase(req.getName()) && repository.existsByNameIgnoreCase(req.getName())) {
            throw new DuplicateResourceException("Agreement type already exists: " + req.getName());
        }
        entity.setName(req.getName());
        if (req.getIsActive() != null) entity.setActive(req.getIsActive());
        entity.setUpdatedByUserId(userId);
        return toResponse(repository.save(entity));
    }

    public void toggleStatus(Long id, Long userId) {
        AgreementType entity = findOrThrow(id);
        entity.setActive(!entity.isActive());
        entity.setUpdatedByUserId(userId);
        repository.save(entity);
    }

    private AgreementType findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("AgreementType", id));
    }

    private Specification<AgreementType> buildSpec(Map<String, String> filters) {
        Specification<AgreementType> spec = SpecificationUtils.empty();
        if (filters == null || filters.isEmpty()) return spec;
        if (filters.containsKey("name")) spec = spec.and(SpecificationUtils.stringLike("name", filters.get("name")));
        if (filters.containsKey("isActive")) spec = spec.and(SpecificationUtils.booleanEquals("isActive", SpecificationUtils.parseBoolean(filters.get("isActive"))));
        return spec;
    }

    private AgreementTypeResponse toResponse(AgreementType e) {
        return AgreementTypeResponse.builder()
                .id(e.getId()).name(e.getName()).isActive(e.isActive())
                .createdAt(e.getCreatedAt()).createdByUserId(e.getCreatedByUserId())
                .updatedAt(e.getUpdatedAt()).updatedByUserId(e.getUpdatedByUserId()).build();
    }
}
