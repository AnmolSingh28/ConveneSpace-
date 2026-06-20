package com.concertbooking.concert_booking.queue.dto;

import java.util.UUID;

public record QueueStatusResponse(
        UUID tierId,
        long queueSize,
        int availableTickets,
        boolean queueActive
) {
}
