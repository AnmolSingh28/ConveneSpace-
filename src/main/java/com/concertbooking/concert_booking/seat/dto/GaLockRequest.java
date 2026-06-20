package com.concertbooking.concert_booking.seat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GaLockRequest {
    @NotNull(message = "Quantity must never be null")
    @Min(value=1,message = "Quantity must be atleast 1")
    private Integer quantity;
}
