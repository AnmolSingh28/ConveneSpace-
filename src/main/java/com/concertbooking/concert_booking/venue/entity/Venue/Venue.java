package com.concertbooking.concert_booking.venue.entity.Venue;


import com.concertbooking.concert_booking.common.enums.VenueType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="venues",indexes = {
        @Index(name="idx_venue_city",columnList = "city"),
        @Index(name="idx_venue_type",columnList = "venue_type"),
        @Index(name = "idx_venue_location",columnList = "latitude,longitude")
})
@EqualsAndHashCode(of="id")
@ToString(exclude = "sections")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String address;

    @Column(name="google_maps_url")
    private String googleMapsURL;

    @Column(name="latitude")
    private Double latitude;

    @Column(name="longitude")
    private Double longitude;

    @Column(name="location_description",columnDefinition = "TEXT")
    private String locationDescription;

    @Column(name = "layout_image_url")
    private String layoutImageUrl;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "venue_type", nullable = false)
    private VenueType venueType;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VenueSection> sections = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;




}
