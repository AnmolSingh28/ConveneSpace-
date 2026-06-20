package com.concertbooking.concert_booking.payment.mapper;

import com.concertbooking.concert_booking.payment.dto.PaymentResponse;
import com.concertbooking.concert_booking.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(source ="booking.id",target="bookingId")
    @Mapping(source="booking.bookingReference",target="bookingReference")
    @Mapping(target="razorpayKeyId",ignore=true)
    @Mapping(target="currency",ignore = true)
    PaymentResponse toResponse(Payment payment);
}
