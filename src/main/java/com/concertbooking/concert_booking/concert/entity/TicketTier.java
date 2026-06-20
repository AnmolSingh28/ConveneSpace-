package com.concertbooking.concert_booking.concert.entity;
import com.concertbooking.concert_booking.common.enums.TierStatus;

import com.concertbooking.concert_booking.venue.entity.Venue.VenueSection;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="ticket_tiers",indexes = {
        @Index(name="idx_tier_concert",columnList = "concert_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of="id")
@ToString(exclude = {"concert","sections"})
public class TicketTier {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version",nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private VenueSection section;

    @Column(name = "tier_name", nullable = false)
    private String tierName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "max_per_user", nullable = false)
    private Integer maxPerUser;

    @Column(name = "sale_start")
    private LocalDateTime saleStart;

    @Column(name = "sale_end")
    private LocalDateTime saleEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier_status", nullable = false)
    private TierStatus tierStatus;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder.Default
    @Column(name = "lock_ttl_minutes", nullable = false)
    private Integer lockTtlMinutes = 10;
}
