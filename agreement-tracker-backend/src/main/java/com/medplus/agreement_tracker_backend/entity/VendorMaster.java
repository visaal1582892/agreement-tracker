package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Mock Phase 1 table. Will be replaced by UNS Vendor Master API integration in Phase 2.
 */
@Entity
@Table(name = "vendor_master", indexes = {
        @Index(name = "idx_vendor_code", columnList = "vendor_code"),
        @Index(name = "idx_vendor_name", columnList = "vendor_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorMaster extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_code", unique = true, length = 50)
    private String vendorCode;

    @Column(name = "vendor_name", nullable = false, length = 255)
    private String vendorName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
