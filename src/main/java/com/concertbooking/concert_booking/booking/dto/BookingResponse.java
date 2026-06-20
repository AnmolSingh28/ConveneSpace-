package com.concertbooking.concert_booking.booking.dto;

import com.concertbooking.concert_booking.common.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookingResponse(

        UUID id,
        String bookingReference,
        BookingStatus status,

        UUID concertId,
        String concertTitle,
        String artistName,
        LocalDateTime concertDate,


        String venueName,
        String venueCity,


        BigDecimal baseAmount,
        BigDecimal platformFee,
        BigDecimal paymentGatewayFee,
        BigDecimal totalAmount,

        List<BookingItemResponse> items,

       
        LocalDateTime cancelledAt,
        String cancellationReason,
        BigDecimal refundAmount,
        LocalDateTime createdAt



) {
}
