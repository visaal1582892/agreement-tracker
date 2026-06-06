package com.medplus.agreement_tracker_backend.service.master;

import com.medplus.agreement_tracker_backend.dto.request.MasterPageRequest;
import com.medplus.agreement_tracker_backend.dto.request.master.ProductMasterRequest;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.dto.response.master.ProductMasterResponse;
import com.medplus.agreement_tracker_backend.entity.DivisionMaster;
import com.medplus.agreement_tracker_backend.entity.ManufacturerMaster;
import com.medplus.agreement_tracker_backend.entity.ProductMaster;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.DivisionMasterRepository;
import com.medplus.agreement_tracker_backend.repository.ManufacturerMasterRepository;
import com.medplus.agreement_tracker_backend.repository.ProductMasterRepository;
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
public class ProductMasterService {

    private final ProductMasterRepository productRepository;
    private final ManufacturerMasterRepository manufacturerRepository;
    private final DivisionMasterRepository divisionRepository;

    @Transactional(readOnly = true)
    public PagedResponse<ProductMasterResponse> search(MasterPageRequest req) {
        Specification<ProductMaster> spec = buildSpec(req.getFilters());
        Sort sort = Sort.by(Sort.Direction.fromString(req.getSortDirection()), req.getSortBy());
        Page<ProductMaster> page = productRepository.findAll(spec, PageRequest.of(req.getPage(), req.getSize(), sort));
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ProductMasterResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    /** Wizard backward-compat: products filtered by vendor + optional manufacturer/division. */
    @Transactional(readOnly = true)
    public List<ProductMasterResponse> findByVendorIds(List<Long> vendorIds) {
        return productRepository.findByVendorIds(vendorIds).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductMasterResponse> findByVendorIdsAndManufacturer(List<Long> vendorIds, Long manufacturerId) {
        return productRepository.findByVendorIdsAndManufacturer(vendorIds, manufacturerId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductMasterResponse> findByVendorIdsAndDivisions(List<Long> vendorIds, List<Long> divisionIds) {
        return productRepository.findByVendorIdsAndDivisions(vendorIds, divisionIds).stream()
                .map(this::toResponse).toList();
    }

    public ProductMasterResponse create(ProductMasterRequest req, Long userId) {
        ManufacturerMaster manufacturer = manufacturerRepository.findById(req.getManufacturerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", req.getManufacturerId()));
        DivisionMaster division = divisionRepository.findById(req.getDivisionId())
                .orElseThrow(() -> new ResourceNotFoundException("Division", req.getDivisionId()));
        ProductMaster entity = ProductMaster.builder()
                .productCode(req.getProductCode())
                .productName(req.getProductName())
                .manufacturer(manufacturer)
                .division(division)
                .build();
        entity.setCreatedByUserId(userId);
        return toResponse(productRepository.save(entity));
    }

    public ProductMasterResponse update(Long id, ProductMasterRequest req, Long userId) {
        ProductMaster entity = findOrThrow(id);
        ManufacturerMaster manufacturer = manufacturerRepository.findById(req.getManufacturerId())
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", req.getManufacturerId()));
        DivisionMaster division = divisionRepository.findById(req.getDivisionId())
                .orElseThrow(() -> new ResourceNotFoundException("Division", req.getDivisionId()));
        entity.setProductCode(req.getProductCode());
        entity.setProductName(req.getProductName());
        entity.setManufacturer(manufacturer);
        entity.setDivision(division);
        if (req.getIsActive() != null) entity.setActive(req.getIsActive());
        entity.setUpdatedByUserId(userId);
        return toResponse(productRepository.save(entity));
    }

    public void toggleStatus(Long id, Long userId) {
        ProductMaster entity = findOrThrow(id);
        entity.setActive(!entity.isActive());
        entity.setUpdatedByUserId(userId);
        productRepository.save(entity);
    }

    private ProductMaster findOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private Specification<ProductMaster> buildSpec(Map<String, String> filters) {
        Specification<ProductMaster> spec = SpecificationUtils.empty();
        if (filters == null || filters.isEmpty()) return spec;
        if (filters.containsKey("productName")) {
            spec = spec.and(SpecificationUtils.stringLike("productName", filters.get("productName")));
        }
        if (filters.containsKey("productCode")) {
            spec = spec.and(SpecificationUtils.stringLike("productCode", filters.get("productCode")));
        }
        if (filters.containsKey("manufacturerId")) {
            spec = spec.and(SpecificationUtils.joinIdEquals("manufacturer",
                    SpecificationUtils.parseLong(filters.get("manufacturerId"))));
        }
        if (filters.containsKey("manufacturerName")) {
            spec = spec.and(SpecificationUtils.joinStringLike("manufacturer", "manufacturerName",
                    filters.get("manufacturerName")));
        }
        if (filters.containsKey("divisionId")) {
            spec = spec.and(SpecificationUtils.joinIdEquals("division",
                    SpecificationUtils.parseLong(filters.get("divisionId"))));
        }
        if (filters.containsKey("divisionName")) {
            spec = spec.and(SpecificationUtils.joinStringLike("division", "divisionName",
                    filters.get("divisionName")));
        }
        if (filters.containsKey("isActive")) {
            spec = spec.and(SpecificationUtils.booleanEquals("isActive",
                    SpecificationUtils.parseBoolean(filters.get("isActive"))));
        }
        return spec;
    }

    private ProductMasterResponse toResponse(ProductMaster e) {
        return ProductMasterResponse.builder()
                .id(e.getId())
                .productCode(e.getProductCode())
                .productName(e.getProductName())
                .manufacturerId(e.getManufacturer() != null ? e.getManufacturer().getId() : null)
                .manufacturerName(e.getManufacturer() != null ? e.getManufacturer().getManufacturerName() : null)
                .divisionId(e.getDivision() != null ? e.getDivision().getId() : null)
                .divisionName(e.getDivision() != null ? e.getDivision().getDivisionName() : null)
                .isActive(e.isActive())
                .createdAt(e.getCreatedAt())
                .createdByUserId(e.getCreatedByUserId())
                .updatedAt(e.getUpdatedAt())
                .updatedByUserId(e.getUpdatedByUserId())
                .build();
    }
}
