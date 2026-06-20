package com.concertbooking.concert_booking.common.exception;

public class PaymentException  extends RuntimeException{
    public PaymentException(String message){
        super(message);
    }
}
