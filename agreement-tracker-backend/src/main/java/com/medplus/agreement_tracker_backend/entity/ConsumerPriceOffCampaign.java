package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import com.medplus.agreement_tracker_backend.enums.PriceOffApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.PriceOffDiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "consumer_price_off_campaign", indexes = {
        @Index(name = "idx_cpo_product_id", columnList = "product_id"),
        @Index(name = "idx_cpo_approval_status", columnList = "approval_status"),
        @Index(name = "idx_cpo_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_cpo_start_date", columnList = "start_date"),
        @Index(name = "idx_cpo_updated_at", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsumerPriceOffCampaign extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductMaster product;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "l3_category", length = 255)
    private String l3Category;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "max_unit_cap")
    private Integer maxUnitCap;

    @Column(name = "from_qty")
    private Integer fromQty;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "campaign_id", length = 100)
    private String campaignId;

    @Column(name = "campaign_id_updated_at")
    private LocalDateTime campaignIdUpdatedAt;

    @Column(name = "campaign_id_updated_by_user_id")
    private Long campaignIdUpdatedByUserId;

    @Column(name = "location_label", nullable = false, length = 255)
    private String locationLabel;

    @Column(name = "channel_label", nullable = false, length = 255)
    private String channelLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private PriceOffDiscountType discountType;

    @Column(name = "cp", nullable = false, precision = 12, scale = 4)
    private BigDecimal cp;

    @Column(name = "mrp", nullable = false, precision = 12, scale = 4)
    private BigDecimal mrp;

    @Column(name = "base_offer", nullable = false, precision = 12, scale = 4)
    private BigDecimal baseOffer;

    @Column(name = "medplus_contribution", nullable = false, precision = 12, scale = 4)
    private BigDecimal medplusContribution;

    @Column(name = "margin_percent", precision = 12, scale = 4)
    private BigDecimal marginPercent;

    @Column(name = "final_offer", precision = 12, scale = 4)
    private BigDecimal finalOffer;

    @Column(name = "percent_off", precision = 12, scale = 4)
    private BigDecimal percentOff;

    @Column(name = "final_margin_percent", precision = 12, scale = 4)
    private BigDecimal finalMarginPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private PriceOffApprovalStatus approvalStatus;

    @Column(name = "units_consumed", nullable = false)
    @Builder.Default
    private Integer unitsConsumed = 0;

    @Column(name = "submitted_by_user_id", nullable = false)
    private Long submittedByUserId;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "rejection_remarks", length = 1000)
    private String rejectionRemarks;

    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @JoinTable(
            name = "consumer_price_off_campaign_states",
            joinColumns = @JoinColumn(name = "campaign_id"),
            inverseJoinColumns = @JoinColumn(name = "state_id")
    )
    @Builder.Default
    private Set<StateMaster> states = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @JoinTable(
            name = "consumer_price_off_campaign_channels",
            joinColumns = @JoinColumn(name = "campaign_id"),
            inverseJoinColumns = @JoinColumn(name = "channel_id")
    )
    @Builder.Default
    private Set<ChannelMaster> channels = new LinkedHashSet<>();
}
