package com.concertbooking.concert_booking.booking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingCreatedEvent {
    private UUID bookingId;
    private String bookingReference;
    private UUID userId;
    private String userEmail;
    private String userName;
    private UUID concertId;
    private String concertTitle;
    private String artistName;
    private LocalDateTime concertDate;
    private String venueName;
    private String venueCity;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private UUID organizerId;
    private String organizerName;
}
