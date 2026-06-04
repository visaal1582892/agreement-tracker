package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Mock Phase 1 table. Will be replaced by UNS Product Master API integration in Phase 2.
 */
@Entity
@Table(name = "division_master", indexes = {
        @Index(name = "idx_div_manufacturer_id", columnList = "manufacturer_id"),
        @Index(name = "idx_div_code", columnList = "division_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DivisionMaster extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "division_code", unique = true, length = 50)
    private String divisionCode;

    @Column(name = "division_name", nullable = false, length = 255)
    private String divisionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id", nullable = false)
    private ManufacturerMaster manufacturer;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
