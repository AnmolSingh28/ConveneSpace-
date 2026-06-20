package com.concertbooking.concert_booking.common.exception;

public class SeatUnavailableException  extends RuntimeException{
    public SeatUnavailableException(String message){
        super(message);
    }
}
