package com.concertbooking.concert_booking.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSuccessEvent {
    private UUID bookingId;
    private String bookingReference;
    private UUID userId;
    private String userEmail;
    private String userName;
    private String concertTitle;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private UUID organizerId;
    private String organizerName;
}
