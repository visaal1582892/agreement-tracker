package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agreement_jbp_version_frequencies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_jbp_ver_freq", columnNames = {"agreement_version_id", "frequency"})
}, indexes = {
        @Index(name = "idx_jbp_ver_freq_version", columnList = "agreement_version_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementJbpVersionFrequency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private AgreementVersion agreementVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 50)
    private PayoutFrequency frequency;
}
