package com.concertbooking.concert_booking.unit.booking;

import com.concertbooking.concert_booking.booking.dto.BookingRequest;
import com.concertbooking.concert_booking.booking.dto.BookingResponse;
import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.mapper.BookingMapper;
import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.booking.service.BookingItemService;
import com.concertbooking.concert_booking.booking.service.BookingPricingService;
import com.concertbooking.concert_booking.booking.service.BookingService;
import com.concertbooking.concert_booking.booking.service.BookingValidationService;

import com.concertbooking.concert_booking.common.enums.BookingStatus;
import com.concertbooking.concert_booking.common.enums.SectionType;
import com.concertbooking.concert_booking.common.exception.BookingException;

import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.ConcertRepository;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;

import com.concertbooking.concert_booking.metrics.MetricsService;
import com.concertbooking.concert_booking.notification.service.NotificationPublisher;

import com.concertbooking.concert_booking.queue.service.VirtualQueueService;

import com.concertbooking.concert_booking.review.repository.OrganizerReviewRepository;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.seat.service.SeatLockService;

import com.concertbooking.concert_booking.user.entity.User;

import com.concertbooking.concert_booking.venue.entity.Venue.Venue;
import com.concertbooking.concert_booking.venue.entity.Venue.VenueSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingItemRepository bookingItemRepository;
    @Mock private TicketTierRepository ticketTierRepository;
    @Mock private SeatInventoryRepository seatInventoryRepository;
    @Mock private BookingValidationService bookingValidationService;
    @Mock private BookingPricingService bookingPricingService;
    @Mock private BookingItemService bookingItemService;
    @Mock private BookingMapper bookingMapper;
    @Mock private VirtualQueueService virtualQueueService;
    @Mock private RedisTemplate<String,Object> redisTemplate;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private SeatLockService seatLockService;
    @Mock private OrganizerReviewRepository organizerReviewRepository;
    @Mock
    private MetricsService metricsService;
    @Mock private ConcertRepository concertRepository;
    private BookingService bookingService;

    @BeforeEach
    void setup() {bookingService=new BookingService(
                bookingRepository,
                bookingItemRepository,
                ticketTierRepository,
                seatInventoryRepository,
                bookingValidationService,
                bookingPricingService,
                bookingItemService,
                bookingMapper,
                virtualQueueService,
                redisTemplate,
                notificationPublisher,
                seatLockService,

            metricsService
        );
    }
    //TEST 1
    @Test
    void createBooking_idempotencyKeyExists(){
        BookingRequest request=new BookingRequest();
        request.setIdempotencyKey("abcd1234");

        Booking booking=Booking.builder().bookingReference("CB-1234").build();

        BookingResponse response=mock(BookingResponse.class);

        when(bookingRepository.existsByIdempotencyKey("abcd1234")).thenReturn(true);
        when(bookingRepository.findByIdempotencyKey("abcd1234")).thenReturn(Optional.of(booking));

        when(bookingMapper.toResponse(booking)).thenReturn(response);

        BookingResponse result =bookingService.createBooking(request,new User());

        assertNotNull(result);

        verify(ticketTierRepository,never()).findById(any());

        verify(bookingRepository,never()).save(any());

    }
    @Test
    void createBooking_queueActive_NoToken() {

        UUID tierId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());

        BookingRequest request = new BookingRequest();
        request.setTierId(tierId);
        request.setIdempotencyKey("abc123");

        TicketTier tier = TicketTier.builder()
                .id(tierId)
                .build();

        when(bookingRepository.existsByIdempotencyKey(anyString()))
                .thenReturn(false);

        when(ticketTierRepository.findById(tierId))
                .thenReturn(Optional.of(tier));

        when(virtualQueueService.isQueueActive(tierId))
                .thenReturn(true);

        when(redisTemplate.hasKey(anyString()))
                .thenReturn(false);

        when(virtualQueueService.hasValidQueueToken(
                eq(tierId),
                eq(user)
        )).thenReturn(false);

        BookingException exception=assertThrows(BookingException.class,()->bookingService.createBooking(request,user));

        assertEquals("Please wait in queue before booking",exception.getMessage());

        verify(bookingRepository, never())
                .save(any());
    }
    @Test
    void cancelBooking_success() {

        UUID bookingId=UUID.randomUUID();
        User user=new User();
        user.setId(UUID.randomUUID());

        Concert concert=new Concert();
        concert.setTitle("Coldplay");
        Booking booking=Booking.builder().user(user).concert(concert)
                .status(BookingStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        when(bookingPricingService.calculateRefund(booking)).thenReturn(BigDecimal.valueOf(500));

        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(mock(BookingResponse.class));

        BookingResponse response=bookingService.cancelBooking(bookingId,user,"changed plans");

        assertNotNull(response);

        assertEquals(BookingStatus.CANCELLED,booking.getStatus());

        verify(bookingRepository).save(booking);
    }
    @Test
    void createBooking_success() {

        UUID tierId = UUID.randomUUID();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@gmail.com");
        user.setName("Jason");

        BookingRequest request = new BookingRequest();
        request.setTierId(tierId);
        request.setQuantity(2);
        request.setIdempotencyKey("idem-123");

        Concert concert = new Concert();
        concert.setId(UUID.randomUUID());
        concert.setTitle("Coldplay");
        concert.setArtistName("Coldplay");

        Venue venue = new Venue();
        venue.setName("Mumbai Stadium");
        venue.setCity("Mumbai");

        concert.setVenue(venue);

        VenueSection section = new VenueSection();
        section.setSectionType(SectionType.GA);

        TicketTier tier = TicketTier.builder()
                .id(tierId)
                .price(BigDecimal.valueOf(1000))
                .concert(concert)
                .section(section)
                .build();

        List<BookingItem> items = new ArrayList<>();

        Booking savedBooking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingReference("CB-123")
                .concert(concert)
                .totalAmount(BigDecimal.valueOf(2200))
                .build();

        BookingResponse response=mock(BookingResponse.class);

        when(bookingRepository.existsByIdempotencyKey(anyString())).thenReturn(false);

        when(ticketTierRepository.findById(tierId)).thenReturn(Optional.of(tier));

        when(virtualQueueService.isQueueActive(tierId)).thenReturn(false);

        when(bookingItemService.buildGaItems(request,tier,user)).thenReturn(items);

        when(bookingPricingService.calculatePlatformFee(any())).thenReturn(BigDecimal.valueOf(100));

        when(bookingPricingService.calculateGatewayFee(any())).thenReturn(BigDecimal.valueOf(100));

        when(bookingPricingService.calculateTotal(any(),any(),any())).thenReturn(BigDecimal.valueOf(2200));

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        when(bookingMapper.toResponse(savedBooking)).thenReturn(response);

        BookingResponse result = bookingService.createBooking(request, user);

        assertNotNull(result);

        verify(bookingValidationService).validateBookingRequest(request, tier, user);

        verify(bookingRepository).save(any(Booking.class));

        verify(bookingItemRepository).saveAll(items);
    }
}
