package com.concertbooking.concert_booking.queue.constants;

public class QueueConstants {
    private QueueConstants(){}

    public static final String QUEUE_PREFIX="queue:tier:";
    public static final String QUEUE_TOKEN_PREFIX="queue:token:";
    public static final int ADMISSION_WINDOW_MINUTES=2;
    public static final int MAX_ADMISSIONS_PER_BATCH=500;

}
