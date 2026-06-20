package com.concertbooking.concert_booking.metrics;

import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.common.enums.SeatStatus;
import com.concertbooking.concert_booking.concert.repository.ConcertRepository;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.user.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

@Service

public class MetricsService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ConcertRepository concertRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final TicketTierRepository ticketTierRepository;
    private final Counter bookingCounter;
    private final Counter revenueCounter;
    private final Counter paymentSuccessCounter;
    private final Counter paymentFailedCounter;
    private final Counter ticketsSoldCounter;
    private final Counter bookingCancellationCounter;
    private final Counter queueEntriesCounter;
    private final AtomicInteger activeQueueUsers;
    private final Counter reservationsTotalCounter;
    private final Counter checkinsCounter;


    public MetricsService(MeterRegistry meterRegistry,BookingRepository bookingRepository,
                          UserRepository userRepository, ConcertRepository concertRepository,
                          SeatInventoryRepository seatInventoryRepository, TicketTierRepository ticketTierRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.concertRepository = concertRepository;
        this.seatInventoryRepository=seatInventoryRepository;
        this.ticketTierRepository=ticketTierRepository;
        this.bookingCounter = Counter.builder("eventmax_bookings_total")
                .description("Total bookings created")
                .register(meterRegistry);
        this.revenueCounter = Counter.builder("eventmax_revenue_total")
                .description("Total revenue earned")
                .register(meterRegistry);
        this.paymentSuccessCounter = Counter.builder("eventmax_payments_success_total")
                .description("Total successful payments")
                .register(meterRegistry);
        this.paymentFailedCounter = Counter.builder("eventmax_payments_failed_total")
                .description("Total failed payments")
                .register(meterRegistry);
        this.ticketsSoldCounter = Counter.builder("eventmax_tickets_sold_total")
                .description("Total tickets sold")
                .register(meterRegistry);
        this.bookingCancellationCounter = Counter.builder("eventmax_booking_cancellations_total")
                .description("Total booking cancellations")
                .register(meterRegistry);
        this.queueEntriesCounter = Counter.builder("eventmax_queue_entries_total")
                .description("Total users entered queue")
                .register(meterRegistry);
        this.activeQueueUsers = meterRegistry.gauge(
                "eventmax_active_queue_users",
                new AtomicInteger(0)
        );
        this.reservationsTotalCounter = Counter.builder("eventmax_reservations_total").description("Total reservations created").register(meterRegistry);
        this.checkinsCounter = Counter.builder("eventmax_checkins_total")
                .description("Total attendee checkins")
                .register(meterRegistry);

        Gauge.builder(
                "eventmax_users_total",
                () -> (double) userRepository.count()
        ).register(meterRegistry);

        meterRegistry.gauge(
                "eventmax_concerts_total",
                concertRepository,
                repo -> (double) repo.count()
        );

        meterRegistry.gauge(
                "eventmax_bookings_db_total",
                bookingRepository,
                repo -> (double) repo.count()
        );
        Gauge.builder(
                "eventmax_seats_sold",
                seatInventoryRepository,
                repo -> (double) repo.countByStatus(SeatStatus.BOOKED)
        ).register(meterRegistry);



        Gauge.builder(
                "eventmax_revenue",
                bookingRepository,
                repo -> repo.totalRevenue().doubleValue()
        ).register(meterRegistry);
    }

    public void incrementBookings() {
        bookingCounter.increment();
    }
    public void addRevenue(BigDecimal amount){
        revenueCounter.increment(amount.doubleValue());
    }
    public void incrementSuccessfulPayments() {
        paymentSuccessCounter.increment();
    }
    public void incrementFailedPayments() {
        paymentFailedCounter.increment();
    }
    public void addTicketsSold(int quantity) {ticketsSoldCounter.increment(quantity);}
    public void incrementBookingCancellations() {bookingCancellationCounter.increment();}
    public void incrementQueueEntries() {queueEntriesCounter.increment();}
    public void incrementActiveQueueUsers() {activeQueueUsers.incrementAndGet();}
    public void decrementActiveQueueUsers() {activeQueueUsers.decrementAndGet();}
   public void incrementReservations() {reservationsTotalCounter.increment();}
    public void incrementCheckins() {checkinsCounter.increment();}

}

