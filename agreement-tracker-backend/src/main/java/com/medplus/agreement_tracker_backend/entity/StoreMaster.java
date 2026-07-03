package com.medplus.agreement_tracker_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stores_master", indexes = {
        @Index(name = "idx_stores_master_state", columnList = "state_id"),
        @Index(name = "idx_stores_master_active", columnList = "is_active")
}, uniqueConstraints = @UniqueConstraint(name = "uk_stores_master_code", columnNames = "store_code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_code", nullable = false, unique = true, length = 50)
    private String storeCode;

    @Column(name = "store_name", nullable = false, length = 255)
    private String storeName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "state_id", nullable = false)
    private StateMaster state;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
