package com.concertbooking.concert_booking.seat.dto;

import com.concertbooking.concert_booking.common.enums.SeatStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

public record SeatResponse (
     UUID id,
     String rowLabel,
     String seatNumber,
     SeatStatus status,
     UUID tierId,
     String tierName,
     java.math.BigDecimal price
)
{}
