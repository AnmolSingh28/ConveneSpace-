package com.concertbooking.concert_booking.concert.dto;

import com.concertbooking.concert_booking.common.enums.ConcertStatus;

import com.concertbooking.concert_booking.concert.entity.EventCategory;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;




public record ConcertSummaryResponse (
    UUID id,
    String title,
    String artistName,
    String bannerImageUrl,
    String venueName,
    String venueCity,
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    LocalDateTime concertDate,
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    LocalDateTime saleStartTime,
    ConcertStatus status,
    boolean isFeatured,
    boolean requiresPreRegistration,
    BigDecimal startingPrice,
    String category,
    Double distanceKm,
    UUID organizerId,
    String organizerName
)
{}
