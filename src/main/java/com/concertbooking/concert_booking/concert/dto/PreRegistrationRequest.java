package com.concertbooking.concert_booking.concert.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PreRegistrationRequest {
    @NotNull(message = "Concert ID is required")
    private UUID concertId;
}
