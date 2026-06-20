package com.concertbooking.concert_booking.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreatePaymentRequest {
    @NotNull(message = "Booking ID is required")
    private UUID bookingId;
}
