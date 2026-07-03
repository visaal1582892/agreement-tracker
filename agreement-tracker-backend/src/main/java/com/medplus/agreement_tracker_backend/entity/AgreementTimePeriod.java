package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.util.TimePeriodDimensions;
import jakarta.persistence.*;
import lombok.*;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "agreement_time_periods", uniqueConstraints = {
        @UniqueConstraint(name = "uk_atp_name", columnNames = "name")
}, indexes = {
        @Index(name = "idx_atp_period_frequency", columnList = "period_frequency")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementTimePeriod extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_frequency", length = 20)
    private PayoutFrequency periodFrequency;

    @OneToMany(mappedBy = "timePeriod", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AgreementTimePeriodMonth> includedMonths = new ArrayList<>();

    public YearMonth earliestIncludedMonth() {
        return TimePeriodDimensions.earliestIncludedMonth(this);
    }

    public YearMonth latestIncludedMonth() {
        return TimePeriodDimensions.latestIncludedMonth(this);
    }

    public static Comparator<AgreementTimePeriod> chronologicalComparator() {
        return Comparator
                .comparing(AgreementTimePeriod::earliestIncludedMonth, Comparator.nullsLast(YearMonth::compareTo))
                .thenComparing(AgreementTimePeriod::getName, Comparator.nullsLast(String::compareTo));
    }
}
