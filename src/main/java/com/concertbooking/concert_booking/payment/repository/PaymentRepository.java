package com.concertbooking.concert_booking.payment.repository;

import com.concertbooking.concert_booking.common.enums.PaymentStatus;
import com.concertbooking.concert_booking.payment.entity.Payment;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByBookingId(UUID bookingId);

    @Profile("prod")
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    boolean existsByBookingIdAndStatus(UUID bookingId, PaymentStatus status);
}
