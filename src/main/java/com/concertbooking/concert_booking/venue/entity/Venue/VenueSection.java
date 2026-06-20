package com.concertbooking.concert_booking.venue.entity.Venue;

import com.concertbooking.concert_booking.common.enums.SectionType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name="venue_section")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of="id")
@ToString(exclude = "venue")
public class VenueSection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="venue_id",nullable = false)
    private Venue venue;

    @Column(nullable = false)
    private String name;

    @Column(name="total_capacity",nullable = false)
    private Integer totalCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name="section_type",nullable = false)
    private SectionType sectionType;

    @Column(name = "x_position")
    private Float xPosition;

    @Column(name = "y_position")
    private Float yPosition;

    @Column(name = "width")
    private Float width;

    @Column(name = "height")
    private Float height;

    @Column(name = "color_hex")
    private String colorHex;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

}
