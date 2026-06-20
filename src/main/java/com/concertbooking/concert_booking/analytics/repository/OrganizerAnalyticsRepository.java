package com.concertbooking.concert_booking.analytics.repository;

import com.concertbooking.concert_booking.analytics.entity.OrganizerAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface OrganizerAnalyticsRepository extends JpaRepository<OrganizerAnalytics,UUID> {
    Optional<OrganizerAnalytics>
    findByOrganizerId(UUID organizerId);
}
