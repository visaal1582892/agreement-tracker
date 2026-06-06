package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agreement_manufacturers", indexes = {
        @Index(name = "idx_am_agreement_id", columnList = "agreement_id"),
        @Index(name = "idx_am_manufacturer_id", columnList = "manufacturer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementManufacturer extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;

    @Column(name = "manufacturer_id", nullable = false)
    private Long manufacturerId;
}
