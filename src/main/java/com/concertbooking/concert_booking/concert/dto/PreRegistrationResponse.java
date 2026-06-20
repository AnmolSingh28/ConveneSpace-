package com.concertbooking.concert_booking.concert.dto;


import java.time.LocalDateTime;
import java.util.UUID;

public record PreRegistrationResponse(
        UUID id,
        UUID concertId,
        String concertTitle,
        Integer queuePosition,
        LocalDateTime purchaseWindowStart,
        LocalDateTime purchaseWindowEnd,
        boolean hasPurchased,
        LocalDateTime createdAt
)
{
}
