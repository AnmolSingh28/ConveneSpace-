package com.concertbooking.concert_booking.concert.dto;

import com.concertbooking.concert_booking.common.enums.ConcertStatus;
import com.concertbooking.concert_booking.common.enums.EventType;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import com.concertbooking.concert_booking.venue.dto.VenueResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ConcertResponse {
    private UUID id;
    private String title;
    private String artistName;
    private String description;
    private String bannerImageUrl;
    private VenueResponse venue;
    private LocalDateTime concertDate;
    private LocalDateTime doorsOpenTime;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private ConcertStatus status;
    private boolean isFeatured;
    private boolean requiresPreRegistration;
    private LocalDateTime preRegistrationStart;
    private LocalDateTime preRegistrationEnd;
    private List<TicketTierResponse> ticketTiers;
    private String category;
    private UUID organizerId;
    private String organizerName;
    private Double organizerRating;

}
