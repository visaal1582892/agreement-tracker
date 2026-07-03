package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import com.medplus.agreement_tracker_backend.enums.AssetCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "agreement_assets", indexes = {
        @Index(name = "idx_aa_agreement_version_id", columnList = "agreement_version_id")
},
        uniqueConstraints = @UniqueConstraint(name = "uk_agreement_assets_version", columnNames = "agreement_version_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementAsset extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_version_id", nullable = false, unique = true)
    private AgreementVersion agreementVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_category", nullable = false, length = 20)
    private AssetCategory assetCategory;

    @Column(name = "asset_type", length = 100)
    private String assetType;

    @Column(name = "store_count")
    private Integer storeCount;

    @Column(name = "payout_per_store", precision = 15, scale = 2)
    private BigDecimal payoutPerStore;

    @Column(name = "flat_payout", precision = 15, scale = 2)
    private BigDecimal flatPayout;

    @Column(name = "remarks", length = 1000)
    private String remarks;
}
