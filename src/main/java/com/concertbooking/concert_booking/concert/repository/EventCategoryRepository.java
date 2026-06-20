package com.concertbooking.concert_booking.concert.repository;

import com.concertbooking.concert_booking.common.enums.EventType;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventCategoryRepository extends JpaRepository<EventCategory, UUID> {
    Optional<EventCategory> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<EventCategory> findByActiveTrue();
}
