package com.concertbooking.concert_booking.booking.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingCancelledEvent {
    private UUID bookingId;
    private String bookingReference;
    private UUID userId;
    private String userEmail;
    private String userName;
    private String concertTitle;
    private BigDecimal refundAmount;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
}
