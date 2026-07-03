package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.enums.MetricType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agreement_actual_metrics", indexes = {
        @Index(name = "idx_actual_metrics_version", columnList = "agreement_version_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_actual_period", columnNames = {
                "agreement_version_id", "time_period_id", "metric_type"
        })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementActualMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private AgreementVersion agreementVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_period_id", nullable = false)
    private AgreementTimePeriod timePeriod;

    @Column(name = "actual_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 20)
    private MetricType metricType;

    @Column(name = "ingested_at")
    private LocalDateTime ingestedAt;

    @PrePersist
    void onCreate() {
        if (ingestedAt == null) {
            ingestedAt = LocalDateTime.now();
        }
    }
}
