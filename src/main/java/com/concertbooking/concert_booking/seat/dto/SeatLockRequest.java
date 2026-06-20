package com.concertbooking.concert_booking.seat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SeatLockRequest {
    @NotNull(message = "Seat ID is required")
    private UUID seatId;
}
