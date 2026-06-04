package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Mock Phase 1 table. Will be replaced by UNS Product Master API integration in Phase 2.
 */
@Entity
@Table(name = "manufacturer_master", indexes = {
        @Index(name = "idx_mfr_code", columnList = "manufacturer_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManufacturerMaster extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manufacturer_code", unique = true, length = 50)
    private String manufacturerCode;

    @Column(name = "manufacturer_name", nullable = false, length = 255)
    private String manufacturerName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
