package com.concertbooking.concert_booking.integration;

import com.concertbooking.concert_booking.ConcertBookingApplication;
import com.concertbooking.concert_booking.booking.dto.BookingRequest;
import com.concertbooking.concert_booking.booking.dto.BookingResponse;
import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.booking.service.BookingService;

import com.concertbooking.concert_booking.common.enums.*;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.ConcertRepository;
import com.concertbooking.concert_booking.concert.repository.EventCategoryRepository;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;

import com.concertbooking.concert_booking.notification.service.NotificationPublisher;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.seat.service.SeatLockService;
import com.concertbooking.concert_booking.user.entity.User;
import com.concertbooking.concert_booking.user.repository.UserRepository;

import com.concertbooking.concert_booking.venue.entity.Venue.Venue;
import com.concertbooking.concert_booking.venue.entity.Venue.VenueSection;
import com.concertbooking.concert_booking.venue.repository.VenueRepository;

import com.concertbooking.concert_booking.venue.repository.VenueSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes= ConcertBookingApplication.class)


@ActiveProfiles("test")
public class BookingIntegrationTest {


    // Autowired Beans

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ConcertRepository concertRepository;
    @Autowired private TicketTierRepository ticketTierRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private VenueSectionRepository venueSectionRepository;

    @Autowired private SeatLockService seatLockService;
    @Autowired private RedisTemplate<String,Object> redisTemplate;
    @Autowired private SeatInventoryRepository seatInventoryRepository;
    @MockBean  private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired private EventCategoryRepository eventCategoryRepository;
    @MockBean private NotificationPublisher notificationPublisher;

    //  Shared Test State

    private User testUser;
    private TicketTier testTier;
    private Concert testConcert;
    private EventCategory testCategory;

    //Setup

    @BeforeEach
    void setUp() {

        bookingRepository.deleteAll();
        seatInventoryRepository.deleteAll();
        ticketTierRepository.deleteAll();
        concertRepository.deleteAll();
        venueSectionRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();
        eventCategoryRepository.deleteAll();


        testUser = userRepository.save(User.builder()
                .name("Test User")
                .email("test@test.com")
                .role(UserRole.USER)
                .emailVerified(true)
                .active(true)
                .build());
        testCategory = eventCategoryRepository.save(
                EventCategory.builder().name("Live Music").active(true).build()
        );
        Venue venue = venueRepository.save(Venue.builder()
                .name("Test Venue")
                .city("Bhopal")
                .address("Test Address")
                .totalCapacity(1000)
                .venueType(VenueType.ESTABLISHED)
                .isActive(true)
                .build());

        VenueSection section = venueSectionRepository.save(VenueSection.builder()
                .venue(venue)
                .name("GA")
                .totalCapacity(1000)
                .sectionType(SectionType.GA)
                .build());

        testConcert=concertRepository.save(Concert.builder()
                .title("Coldplay")
                .artistName("Coldplay")
                .description("Test Concert")
                .venue(venue)
                .organizer(testUser)
                .category(testCategory)
                .status(ConcertStatus.PUBLISHED)
                .saleStartTime(LocalDateTime.now().minusDays(1))
                .saleEndTime(LocalDateTime.now().plusDays(1))
                .concertDate(LocalDateTime.now().plusDays(30))
                .build());

        testTier=ticketTierRepository.save(TicketTier.builder()
                .concert(testConcert)
                .section(section)
                .tierName("General Admission")
                .price(BigDecimal.valueOf(1000))
                .totalQuantity(100)
                .availableQuantity(100)
                .maxPerUser(5)
                .tierStatus(TierStatus.ACTIVE)
                .isActive(true)
                .build());
    }

    // Helper

    private BookingResponse createAndPersistBooking(User user, TicketTier tier, int quantity) {
        seatLockService.lockGaTier(tier.getId(), quantity, user);
        BookingRequest request = new BookingRequest();
        request.setTierId(tier.getId());
        request.setQuantity(quantity);
        request.setIdempotencyKey(UUID.randomUUID().toString());
        return bookingService.createBooking(request, user);
    }


    @Test
    void contextLoads() {
        assertNotNull(bookingService);
        assertNotNull(bookingRepository);
        assertNotNull(userRepository);
        assertNotNull(concertRepository);
        assertNotNull(ticketTierRepository);
    }

    @Test
    void createBooking_persistsBookingInDatabase() {
        seatLockService.lockGaTier(testTier.getId(), 2, testUser);

        BookingRequest request=new BookingRequest();
        request.setTierId(testTier.getId());
        request.setQuantity(2);
        request.setIdempotencyKey(UUID.randomUUID().toString());
        BookingResponse response=bookingService.createBooking(request, testUser);
        assertNotNull(response);
        assertEquals(1, bookingRepository.count());

        Booking saved = bookingRepository.findAll().getFirst();
        assertEquals(testUser.getId(), saved.getUser().getId());
        assertEquals(BookingStatus.PENDING, saved.getStatus());
        assertEquals(request.getIdempotencyKey(), saved.getIdempotencyKey());
    }

    @Test
    void createBooking_duplicateIdempotencyKey_returnsExistingBooking() {
        String key=UUID.randomUUID().toString();
        seatLockService.lockGaTier(testTier.getId(), 2, testUser);
        BookingRequest request1=new BookingRequest();
        request1.setTierId(testTier.getId());
        request1.setQuantity(2);
        request1.setIdempotencyKey(key);
        BookingResponse first=bookingService.createBooking(request1,testUser);

        BookingRequest request2=new BookingRequest();
        request2.setTierId(testTier.getId());
        request2.setQuantity(2);
        request2.setIdempotencyKey(key);
        BookingResponse second=bookingService.createBooking(request2,testUser);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(1,bookingRepository.count());
        assertEquals(first.id(),second.id()); // same booking returned
    }

    @Test
    void cancelBooking_updatesStatusInDatabase() {

        BookingResponse created = createAndPersistBooking(testUser,testTier,2);

        bookingService.cancelBooking(created.id(),testUser,"Changed plans");

        Booking cancelled = bookingRepository.findById(created.id()).orElseThrow();

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
        assertEquals("Changed plans", cancelled.getCancellationReason());
        assertNotNull(cancelled.getRefundAmount());
    }


    @Test
    void cancelBooking_releasesRedisLockAfterCancellation() {
        String userLockKey      = "tier:user:" + testTier.getId() + ":" + testUser.getId();
        String reservedCountKey = "tier:reserved:" + testTier.getId();
        String expiryZsetKey    = "tier:expirations:" + testTier.getId();

        seatLockService.lockGaTier(testTier.getId(), 2, testUser);


        assertNotNull(redisTemplate.opsForValue().get(userLockKey));
        assertEquals(2, Integer.parseInt(redisTemplate.opsForValue().get(reservedCountKey).toString()));
        assertNotNull(redisTemplate.opsForZSet().score(expiryZsetKey, testUser.getId().toString()));

        BookingRequest request = new BookingRequest();
        request.setTierId(testTier.getId());
        request.setQuantity(2);
        request.setIdempotencyKey(UUID.randomUUID().toString());
        BookingResponse created = bookingService.createBooking(request, testUser);

        bookingService.cancelBooking(created.id(), testUser, "Changed plans");


        assertNull(redisTemplate.opsForValue().get(userLockKey));
        Object reserved = redisTemplate.opsForValue().get(reservedCountKey);
        assertEquals(0, reserved != null ? Integer.parseInt(reserved.toString()) : 0);
        assertNull(redisTemplate.opsForZSet().score(expiryZsetKey, testUser.getId().toString()));
    }

    @Test
    void cancelBooking_restoresAvailableQuantityInTicketTier() {

        BookingResponse created = createAndPersistBooking(testUser, testTier, 2);
        bookingService.cancelBooking(created.id(), testUser, "Changed plans");
        TicketTier afterCancellation = ticketTierRepository.findById(testTier.getId()).orElseThrow();
        assertEquals(100, afterCancellation.getAvailableQuantity(),
                "DB quantity must not change for PENDING booking cancellation");
    }

    @Test
    void cancelBooking_alreadyCancelled_throwsException() {

        BookingResponse created = createAndPersistBooking(testUser, testTier, 2);

        bookingService.cancelBooking(created.id(),testUser, "First cancel");

        assertThrows(Exception.class,
                () -> bookingService.cancelBooking(created.id(), testUser, "Second cancel"),
                "Cancelling an already-cancelled booking must throw");
    }

    @Test
    void cancelBooking_byDifferentUser_throwsException() {

        User otherUser = userRepository.save(User.builder()
                .name("Other User")
                .email("other@test.com")
                .role(UserRole.USER)
                .emailVerified(true)
                .active(true)
                .build());

        BookingResponse created = createAndPersistBooking(testUser, testTier, 2);

        assertThrows(Exception.class,
                () -> bookingService.cancelBooking(created.id(), otherUser, "Not my booking"),
                "A user must not cancel another user's booking");
    }

    @Test
    void createBooking_withoutSeatLock_throwsException() {

        BookingRequest request = new BookingRequest();
        request.setTierId(testTier.getId());
        request.setQuantity(2);
        request.setIdempotencyKey(UUID.randomUUID().toString());

        assertThrows(Exception.class,
                () -> bookingService.createBooking(request, testUser),
                "Booking without a seat lock must fail");

        assertEquals(0, bookingRepository.count(),
                "No booking should be persisted when seat lock is missing");
    }

    @Test
    void createBooking_quantityExceedsLock_throwsException() {

        seatLockService.lockGaTier(testTier.getId(), 2, testUser);

        BookingRequest request = new BookingRequest();
        request.setTierId(testTier.getId());
        request.setQuantity(3); // more than locked
        request.setIdempotencyKey(UUID.randomUUID().toString());

        assertThrows(Exception.class,
                () -> bookingService.createBooking(request, testUser),
                "Booking quantity exceeding locked quantity must fail");

        assertEquals(0, bookingRepository.count());
    }

    @Test
    void createBooking_quantityExceedsMaxPerUser_throwsException() {

        BookingRequest request = new BookingRequest();
        request.setTierId(testTier.getId());
        request.setQuantity(6); // maxPerUser is 5
        request.setIdempotencyKey(UUID.randomUUID().toString());

        assertThrows(Exception.class,
                () -> bookingService.createBooking(request, testUser),
                "Booking exceeding maxPerUser limit must fail");
    }

    @Test
    void createMultipleBookings_differentUsers_allPersistCorrectly() {

        User user2 = userRepository.save(User.builder()
                .name("User Two")
                .email("user2@test.com")
                .role(UserRole.USER)
                .emailVerified(true)
                .active(true)
                .build());

        BookingResponse booking1 = createAndPersistBooking(testUser, testTier, 2);
        BookingResponse booking2 = createAndPersistBooking(user2, testTier, 3);


        TicketTier updated = ticketTierRepository.findById(testTier.getId()).orElseThrow();

    }

    @Test
    void createBooking_concurrentSameUser_idempotencyPreventsDoubleBooking() throws Exception {

        String idempotencyKey = UUID.randomUUID().toString();
        seatLockService.lockGaTier(testTier.getId(), 2, testUser);

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Exception> errors = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    BookingRequest req = new BookingRequest();
                    req.setTierId(testTier.getId());
                    req.setQuantity(2);
                    req.setIdempotencyKey(idempotencyKey);
                    bookingService.createBooking(req, testUser);
                } catch (Exception e) {
                    synchronized (errors) { errors.add(e); }
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);


        assertEquals(1, bookingRepository.count(),
                "Idempotency key must prevent duplicate bookings under concurrency");
    }

}
