package com.medplus.agreement_tracker_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Mock Phase 1 table. Maps which products are available under which vendor.
 * Will be replaced by UNS API integration in Phase 2.
 */
@Entity
@Table(name = "vendor_product_mappings", indexes = {
        @Index(name = "idx_vpm_vendor_id", columnList = "vendor_id"),
        @Index(name = "idx_vpm_product_id", columnList = "product_id")
},
uniqueConstraints = @UniqueConstraint(name = "uk_vendor_product", columnNames = {"vendor_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorProductMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorMaster vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductMaster product;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
