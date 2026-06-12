package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "state_master", indexes = {
        @Index(name = "idx_state_name", columnList = "state_name"),
        @Index(name = "idx_state_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateMaster extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_name", nullable = false, length = 255)
    private String stateName;

    @Column(name = "state_code", nullable = false, unique = true, length = 10)
    private String stateCode;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
