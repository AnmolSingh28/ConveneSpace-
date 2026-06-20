package com.concertbooking.concert_booking.queue.dto;

import java.util.UUID;

public record QueuePositionResponse(
        UUID tierId,
        int position,
        long peopleAhead,
        long totalInQueue,
        long estimatedWaitMinutes
) {
}
