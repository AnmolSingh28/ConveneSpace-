package com.concertbooking.concert_booking.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentFailedEvent {
    private UUID bookingId;
    private String bookingReference;
    private UUID userId;
    private String userEmail;
    private String failureReason;
    private LocalDateTime failedAt;
}
