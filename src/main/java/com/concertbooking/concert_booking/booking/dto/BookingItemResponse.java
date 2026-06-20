package com.concertbooking.concert_booking.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingItemResponse(
      UUID id,
      String tierName,
      String sectionName,
      String rowLabel,
      String seatNumber,
      Integer quantity,
      BigDecimal priceAtBooking,
      String qrCodeUrl,
      boolean checkedIn,
      LocalDateTime checkedInAt
) {
}
