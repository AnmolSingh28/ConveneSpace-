package com.concertbooking.concert_booking.integration;

import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.common.enums.*;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.ConcertRepository;
import com.concertbooking.concert_booking.concert.repository.EventCategoryRepository;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.payment.entity.Payment;
import com.concertbooking.concert_booking.payment.repository.PaymentRepository;
import com.concertbooking.concert_booking.queue.dto.QueueJoinResponse;
import com.concertbooking.concert_booking.queue.service.VirtualQueueService;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest

@ActiveProfiles("test")
public class QueueIntegrationTest
{
    @Autowired
    private VirtualQueueService virtualQueueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private VenueSectionRepository venueSectionRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private TicketTierRepository ticketTierRepository;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private SimpMessagingTemplate simpMessagingTemplate;

    @MockBean
    private EventCategoryRepository eventCategoryRepository;

    private User user;
    private Venue venue;
    private VenueSection section;
    private Concert concert;
    private TicketTier tier;
    private Booking booking;
    private Payment payment;
    private BookingItem bookingItem;
    @BeforeEach
    void setup() {
        redisTemplate.getConnectionFactory()
                .getConnection()
                .flushAll();

        user = userRepository.save(
                User.builder()
                        .name("Test User")
                        .email(UUID.randomUUID() + "@test.com")
                        .role(UserRole.USER)
                        .emailVerified(true)
                        .active(true)
                        .build()
        );
        EventCategory testCategory = eventCategoryRepository.save(
                EventCategory.builder()
                        .name("Queue Test Category " + UUID.randomUUID())
                        .active(true)
                        .build()
        );
        venue = venueRepository.save(
                Venue.builder()
                        .name("Test Venue")
                        .city("Bhopal")
                        .address("Test Address")
                        .totalCapacity(1000)
                        .venueType(VenueType.ESTABLISHED)
                        .build()
        );

        section = venueSectionRepository.save(
                VenueSection.builder()
                        .venue(venue)
                        .name("VIP Section")
                        .totalCapacity(100)
                        .sectionType(SectionType.GA)
                        .build()
        );

        concert = concertRepository.save(
                Concert.builder()
                        .title("Test Concert")
                        .artistName("Artist")
                        .description("Description")
                        .venue(venue)
                        .organizer(user)
                        .concertDate(LocalDateTime.now().plusDays(10))
                        .saleStartTime(LocalDateTime.now())
                        .status(ConcertStatus.PUBLISHED)
                        .category(testCategory)
                        .build()
        );

        tier = ticketTierRepository.save(
                TicketTier.builder()
                        .concert(concert)
                        .section(section)
                        .tierName("VIP")
                        .price(BigDecimal.valueOf(1000))
                        .totalQuantity(100)
                        .availableQuantity(100)
                        .maxPerUser(5)
                        .tierStatus(TierStatus.ACTIVE)
                        .build()
        );

        booking = bookingRepository.save(
                Booking.builder()
                        .bookingReference(UUID.randomUUID().toString())
                        .user(user)
                        .concert(concert)
                        .status(BookingStatus.PENDING)
                        .baseAmount(BigDecimal.valueOf(1000))
                        .platformFee(BigDecimal.ZERO)
                        .paymentGatewayFee(BigDecimal.ZERO)
                        .totalAmount(BigDecimal.valueOf(1000))
                        .build()
        );
        String orderId = "order-" + UUID.randomUUID();
        payment = paymentRepository.save(
                Payment.builder()
                        .booking(booking)
                        .amount(BigDecimal.valueOf(2000))
                        .status(PaymentStatus.INITIATED)
                        .razorpayOrderId(orderId)
                        .attemptCount(1)
                        .build()
        );
        bookingItem = bookingItemRepository.save(
                BookingItem.builder()
                        .booking(booking)
                        .tier(tier)
                        .quantity(2)
                        .priceAtBooking(BigDecimal.valueOf(1000))
                        .build()
        );

        booking.getItems().add(bookingItem);
    }
    @Test
    void userShouldJoinQueueSuccessfully() {

        QueueJoinResponse response = virtualQueueService.joinQueue(tier.getId(), user);

        assertNotNull(response);

        assertEquals(tier.getId(), response.tierId());

        assertEquals(1, response.position());

        assertEquals(0, response.peopleAhead());

        assertEquals(1, response.totalInQueue());

        assertEquals("Joined success", response.message());

        assertEquals(1, virtualQueueService.getQueueSize(tier.getId()));
    }
    @Test
    void duplicateJoinShouldNotCreateDuplicateQueueEntry() {

        virtualQueueService.joinQueue(tier.getId(), user);

        QueueJoinResponse response = virtualQueueService.joinQueue(tier.getId(), user);

        assertEquals("Already in queue", response.message());

        assertEquals(1, response.position());

        assertEquals(0, response.peopleAhead());

        assertEquals(1, response.totalInQueue());

        assertEquals(1, virtualQueueService.getQueueSize(tier.getId()));
    }
    @Test
    void admittedUserShouldReceiveToken() {

        virtualQueueService.joinQueue(tier.getId(), user);

        assertEquals(1, virtualQueueService.getQueueSize(tier.getId()));

        virtualQueueService.admitNextUsers(tier.getId(), 1);

        assertTrue(virtualQueueService.hasValidQueueToken(tier.getId(), user));

        assertEquals(0, virtualQueueService.getQueueSize(tier.getId()));

        assertEquals(1, virtualQueueService.countActiveAdmissions(tier.getId()));
    }
    @Test
    void tokenShouldBeConsumedSuccessfully() {

        virtualQueueService.joinQueue(tier.getId(), user);

        virtualQueueService.admitNextUsers(tier.getId(), 1);

        assertTrue(virtualQueueService.hasValidQueueToken(tier.getId(), user));

        virtualQueueService.consumeToken(tier.getId(), user);

        assertFalse(virtualQueueService.hasValidQueueToken(tier.getId(), user));

        assertEquals(1, virtualQueueService.countActiveAdmissions(tier.getId()));
    }
    @Test
    void userShouldLeaveQueueSuccessfully() {

        virtualQueueService.joinQueue(tier.getId(), user);

        assertEquals(1, virtualQueueService.getQueueSize(tier.getId()));

        virtualQueueService.leaveQueue(tier.getId(), user);

        assertEquals(0, virtualQueueService.getQueueSize(tier.getId())
        );
    }
}
