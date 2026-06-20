package com.concertbooking.concert_booking.unit.payment;
import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.common.enums.BookingStatus;
import com.concertbooking.concert_booking.common.enums.PaymentStatus;
import com.concertbooking.concert_booking.common.exception.PaymentException;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.metrics.MetricsService;
import com.concertbooking.concert_booking.notification.service.NotificationPublisher;
import com.concertbooking.concert_booking.payment.dto.PaymentResponse;
import com.concertbooking.concert_booking.payment.entity.Payment;
import com.concertbooking.concert_booking.payment.mapper.PaymentMapper;
import com.concertbooking.concert_booking.payment.repository.PaymentRepository;
import com.concertbooking.concert_booking.payment.service.PaymentService;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.seat.service.SeatLockService;
import com.concertbooking.concert_booking.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;

    @Mock private BookingRepository bookingRepository;

    @Mock private SeatInventoryRepository seatInventoryRepository;

    @Mock private TicketTierRepository ticketTierRepository;

    @Mock private PaymentMapper paymentMapper;

    @Mock private NotificationPublisher notificationPublisher;

    @Mock private SeatLockService seatLockService;

    @Mock private  MetricsService metricsService;
    private PaymentService paymentService;


    @BeforeEach
    void setup(){
    paymentService = new PaymentService(
            paymentRepository,
            bookingRepository,
            seatInventoryRepository,
            ticketTierRepository,
            paymentMapper,
            metricsService,
            notificationPublisher,
            seatLockService
            );
    }
    @Test
    void getPaymentByBooking_success(){
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        Booking booking=Booking.builder().id(bookingId).user(user).build();

        Payment payment=Payment.builder().id(UUID.randomUUID()).booking(booking).build();

        PaymentResponse response = mock(PaymentResponse.class);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

        when(paymentMapper.toResponse(payment)).thenReturn(response);

        PaymentResponse result=paymentService.getPaymentByBooking(bookingId, userId);

        assertNotNull(result);

        verify(paymentMapper).toResponse(payment);
    }
    @Test
    void getPaymentByBooking_accessDenied(){
        UUID bookingId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        Booking booking=Booking.builder().user(owner).build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        PaymentException exception=assertThrows(PaymentException.class,
                        ()->paymentService.getPaymentByBooking(bookingId, UUID.randomUUID())
                );

        assertEquals("Access denied", exception.getMessage());

        verify(paymentRepository, never()).findByBookingId(any());
    }
    @Test
    void simulatePaymentSuccess_alreadySuccessfull(){
        UUID bookingId=UUID.randomUUID();
        Booking booking=Booking.builder().id(bookingId).status(BookingStatus.PENDING).build();

        Payment payment=Payment.builder().status(PaymentStatus.SUCCESS).build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

        assertDoesNotThrow(()->paymentService.simulatePaymentSuccess(bookingId));

        verify(paymentRepository, never()).save(any());
    }
    @Test
   void simulatePaymentSuccess_bookingNotPending(){
        UUID bookingId=UUID.randomUUID();

        Booking booking=Booking.builder().id(bookingId).status(BookingStatus.CONFIRMED).build();

        Payment payment=Payment.builder().status(PaymentStatus.INITIATED).build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

        assertDoesNotThrow(()->paymentService.simulatePaymentSuccess(bookingId)
        );

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleWebhook_invalidSignature(){
        String payload="{\"event\":\"payment.captured\"}";

        String fakeSignature="totally_fake_signature";

        PaymentException exception=assertThrows(PaymentException.class,
                        ()->paymentService.handleWebhook(payload, fakeSignature)
                );

        assertEquals("Invalid webhook signature", exception.getMessage());

        verify(paymentRepository,never()).save(any());
    }

}
