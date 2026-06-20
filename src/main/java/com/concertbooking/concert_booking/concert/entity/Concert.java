package com.concertbooking.concert_booking.concert.entity;

import com.concertbooking.concert_booking.common.enums.ConcertStatus;
import com.concertbooking.concert_booking.common.enums.EventType;
import com.concertbooking.concert_booking.user.entity.User;
import com.concertbooking.concert_booking.venue.entity.Venue.Venue;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="concerts",indexes = {
        @Index(name="idx_concert_date",columnList = "concert_date"),
        @Index(name="idx_concert_status",columnList = "status"),
        @Index(name="idx_concert_artist",columnList = "artist_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of="id")
@Builder
@ToString(exclude={"venue","organizer","ticketTiers"})

public class Concert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name="artist_name",nullable = false)
    private String artistName;

    @Column(nullable = false,columnDefinition = "TEXT")
    private String description;

    @Column(name="banner_image_url",columnDefinition = "TEXT")
    private String bannerImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="venue_id",nullable = false)
    private Venue venue;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="organizer_id",nullable = false)
    private User organizer;

    @Column(name = "concert_date",nullable = false)
    private LocalDateTime concertDate;

    @Column(name = "doors_open_time")
    private LocalDateTime doorsOpenTime;

    @Column(name = "sale_start_time",nullable = false)
    private LocalDateTime saleStartTime;

    @Column(name = "sale_end_time")
    private LocalDateTime saleEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConcertStatus status;

    @Column(name = "is_featured",nullable = false)
    private boolean isFeatured = false;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_category_id")
    private EventCategory category;

    @Column(name = "requires_pre_registration",nullable = false)
    private boolean requiresPreRegistration = false;

    @Column(name = "pre_registration_start")
    private LocalDateTime preRegistrationStart;

    @Column(name = "pre_registration_end")
    private LocalDateTime preRegistrationEnd;

    @OneToMany(mappedBy = "concert",cascade = CascadeType.ALL,orphanRemoval = true)
    @Builder.Default
    private List<TicketTier> ticketTiers = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at",updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


}
