package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "channel_master", indexes = {
        @Index(name = "idx_channel_code", columnList = "channel_code"),
        @Index(name = "idx_channel_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelMaster extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_code", nullable = false, unique = true, length = 50)
    private String channelCode;

    @Column(name = "channel_name", nullable = false, length = 255)
    private String channelName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
