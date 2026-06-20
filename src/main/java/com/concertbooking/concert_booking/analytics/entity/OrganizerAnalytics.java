package com.concertbooking.concert_booking.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizer_analytics",indexes = {
                @Index(name = "idx_analytics_organizer", columnList = "organizer_id")}
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class OrganizerAnalytics {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organizer_id", nullable = false, unique = true)
    private UUID organizerId;

    @Column(nullable = false)
    private long totalEvents=0;

    @Column(nullable = false)
    private long totalBookings=0;

    @Column(nullable = false)
    private long totalTicketsSold=0;

    @Column(nullable = false,precision=15, scale=2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(nullable = false)
    private long totalReviews=0;

    @Column(nullable = false)
    private double averageRating=0.0;

    @Column(nullable = false)
    private long checkedInCount=0;

    @Column(nullable = false)
    private double attendanceRate=0.0;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
