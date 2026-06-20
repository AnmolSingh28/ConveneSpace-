package com.concertbooking.concert_booking.payment.dto;

import com.concertbooking.concert_booking.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID bookingId,
        String bookingReference,
        BigDecimal amount,
        PaymentStatus status,
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpayKeyId,
        String currency,
        String paymentMethod,
        BigDecimal refundAmount,
        LocalDateTime refundedAt,
        String failureReason,
        Integer attemptCount,
        LocalDateTime createdAt
) {
}
