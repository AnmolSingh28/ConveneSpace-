package com.concertbooking.concert_booking.concert.dto;

import java.util.UUID;

public record EventCategoryResponse(
        UUID id,
        String name,
        boolean active
) {
}
