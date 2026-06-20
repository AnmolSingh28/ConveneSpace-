package com.concertbooking.concert_booking.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BookingRequest {

    @NotNull(message = "Tier ID is needed ")
    private UUID tierId;

    private List<UUID> seatIds;

    @Min(value = 1,message = "Quantity should not be less than 1")
    private Integer quantity;

    @NotNull(message = "Idempotency key is necessary")
    private String idempotencyKey;
}
