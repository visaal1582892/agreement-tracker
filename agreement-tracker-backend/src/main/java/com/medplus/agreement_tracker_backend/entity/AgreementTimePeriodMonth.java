package com.medplus.agreement_tracker_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agreement_time_period_months", indexes = {
        @Index(name = "idx_atpm_time_period_id", columnList = "time_period_id"),
        @Index(name = "idx_atpm_calendar_year_month", columnList = "calendar_year, calendar_month")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_atpm_period_year_month",
        columnNames = {"time_period_id", "calendar_year", "calendar_month"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementTimePeriodMonth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_period_id", nullable = false)
    private AgreementTimePeriod timePeriod;

    @Column(name = "calendar_month", nullable = false)
    private Integer calendarMonth;

    @Column(name = "calendar_year", nullable = false)
    private Integer calendarYear;
}
