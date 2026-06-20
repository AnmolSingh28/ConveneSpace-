package com.concertbooking.concert_booking.booking.service;


import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.common.enums.BookingStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Slf4j
public class BookingPricingService {

    @Value("${app.booking.platform-fee-percentage}")
    private BigDecimal platformFeePercentage;

    @Value("${app.booking.gateway-fee-percentage}")
    private BigDecimal gatewayFeePercentage;

    public BigDecimal calculatePlatformFee(BigDecimal baseAmount){
        return baseAmount
                .multiply(platformFeePercentage)
                .divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
    }

    public BigDecimal calculateGatewayFee(BigDecimal baseAmount){
        return baseAmount
                .multiply(gatewayFeePercentage)
                .divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
    }
    public BigDecimal calculateTotal(BigDecimal baseAmount,BigDecimal platformFee,BigDecimal gatewayFee){
        return baseAmount.add(platformFee).add(gatewayFee);
    }
    public  BigDecimal calculateRefund(Booking booking){
        if(booking.getStatus()==BookingStatus.PENDING){
            return booking.getTotalAmount();
        }

        LocalDateTime concertDate=booking.getConcert().getConcertDate();
        long hoursUntil=java.time.Duration
                .between(LocalDateTime.now(),concertDate)
                .toHours();

        if(hoursUntil>=48) {
            return booking.getTotalAmount();
        }else if(hoursUntil>=24) {
            return booking.getTotalAmount()
                    .multiply(BigDecimal.valueOf(0.5))
                    .setScale(2,RoundingMode.HALF_UP);
        }else{
            return BigDecimal.ZERO;
        }
    }
}
