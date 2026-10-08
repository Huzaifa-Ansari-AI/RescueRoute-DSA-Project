package com.huzaifa.rescueroute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Blood bank inventory details entity.
 */
@Entity
@Table(name = "blood_banks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloodBank {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", unique = true, nullable = false)
    private Facility facility;

    @Column(name = "o_positive", nullable = false)
    @Builder.Default
    private boolean oPositive = false;

    @Column(name = "o_negative", nullable = false)
    @Builder.Default
    private boolean oNegative = false;

    @Column(name = "a_positive", nullable = false)
    @Builder.Default
    private boolean aPositive = false;

    @Column(name = "a_negative", nullable = false)
    @Builder.Default
    private boolean aNegative = false;

    @Column(name = "b_positive", nullable = false)
    @Builder.Default
    private boolean bPositive = false;

    @Column(name = "b_negative", nullable = false)
    @Builder.Default
    private boolean bNegative = false;

    @Column(name = "ab_positive", nullable = false)
    @Builder.Default
    private boolean abPositive = false;

    @Column(name = "ab_negative", nullable = false)
    @Builder.Default
    private boolean abNegative = false;

    @Column(name = "stock_updated_at")
    private Instant stockUpdatedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
