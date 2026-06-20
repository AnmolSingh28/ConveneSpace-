package com.concertbooking.concert_booking.venue.repository;

import com.concertbooking.concert_booking.common.enums.SectionType;
import com.concertbooking.concert_booking.venue.entity.Venue.VenueSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface VenueSectionRepository extends JpaRepository<VenueSection, UUID> {
    List<VenueSection> findByVenueId(UUID venueId);
    List<VenueSection> findByVenueIdAndIsActiveTrue(UUID venueId);
    List<VenueSection> findByVenueIdAndSectionType(UUID venueId, SectionType sectionType);
}
