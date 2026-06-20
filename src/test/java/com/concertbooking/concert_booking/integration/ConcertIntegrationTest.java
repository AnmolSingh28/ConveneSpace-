package com.concertbooking.concert_booking.integration;

import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.booking.service.BookingService;
import com.concertbooking.concert_booking.common.enums.*;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.concert.dto.ConcertRequest;
import com.concertbooking.concert_booking.concert.dto.ConcertResponse;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.ConcertRepository;
import com.concertbooking.concert_booking.concert.repository.EventCategoryRepository;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.concert.service.ConcertService;
import com.concertbooking.concert_booking.payment.entity.Payment;
import com.concertbooking.concert_booking.payment.repository.PaymentRepository;
import com.concertbooking.concert_booking.payment.service.PaymentService;
import com.concertbooking.concert_booking.seat.service.SeatLockService;
import com.concertbooking.concert_booking.user.entity.User;
import com.concertbooking.concert_booking.user.repository.UserRepository;
import com.concertbooking.concert_booking.venue.entity.Venue.Venue;
import com.concertbooking.concert_booking.venue.entity.Venue.VenueSection;
import com.concertbooking.concert_booking.venue.repository.VenueRepository;
import com.concertbooking.concert_booking.venue.repository.VenueSectionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@ActiveProfiles("test")
public class ConcertIntegrationTest {

    @Autowired
    private BookingService bookingService;
    @Autowired private PaymentService paymentService;
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private VenueSectionRepository venueSectionRepository;
    @Autowired private ConcertRepository concertRepository;
    @Autowired private TicketTierRepository ticketTierRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private BookingItemRepository bookingItemRepository;
    @Autowired private ConcertService concertService;
    @Autowired
    private EventCategoryRepository eventCategoryRepository;
    private static final String webhookSecret = "hello28@";
    @Autowired private SeatLockService seatLockService;
    @MockBean

    @PersistenceContext
    private EntityManager entityManager;

    private User user;
    private User organizer;
    private TicketTier tier;
    private Concert concert;
    private Booking booking;
    private Payment payment;
    private Venue venue;
    private VenueSection section;
    private BookingItem bookingItem;
    private EventCategory testCategory;
    @BeforeEach
    void setup() {
        bookingItemRepository.deleteAll();
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        ticketTierRepository.deleteAll();
        concertRepository.deleteAll();
        venueSectionRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();
        eventCategoryRepository.deleteAll();
        user = userRepository.save(
                User.builder()
                        .name("Test User")
                        .email("test-" + UUID.randomUUID() + "@test.com")
                        .role(UserRole.USER)
                        .emailVerified(true)
                        .active(true)
                        .build()
        );


        testCategory = eventCategoryRepository.save(EventCategory.builder().name("Live Music").active(true)
                        .build());

        venue = venueRepository.save(
                Venue.builder()
                        .name("Test Venue")
                        .city("Bhopal")
                        .address("Test Address")
                        .totalCapacity(1000)
                        .venueType(VenueType.ESTABLISHED)
                        .build()
        );
        organizer = userRepository.save(
                User.builder()
                        .name("Organizer")
                        .email("organizer-" + UUID.randomUUID() + "@test.com")
                        .password("password")
                        .role(UserRole.ORGANIZER)
                        .active(true)
                        .emailVerified(true)
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
                        .organizer(organizer)
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
    void createConcertShouldCreateDraftConcert() {

        ConcertRequest request = new ConcertRequest();
        request.setTitle("Coldplay");
        request.setArtistName("Coldplay");
        request.setDescription("Music");
        request.setVenueId(venue.getId());
        request.setCategoryId(testCategory.getId());
        request.setConcertDate(LocalDateTime.now().plusDays(30));
        request.setSaleStartTime(LocalDateTime.now().plusDays(1));
        request.setSaleEndTime(LocalDateTime.now().plusDays(20));

        ConcertResponse response = concertService.createConcert(request, organizer);

        assertNotNull(response.getId());
        assertEquals("Coldplay", response.getTitle());

        Concert saved = concertRepository.findById(response.getId()).orElseThrow();

        assertEquals(ConcertStatus.DRAFT, saved.getStatus());
    }

    @Test
    void createConcertShouldThrowWhenVenueNotFound() {

        ConcertRequest request = new ConcertRequest();
        request.setVenueId(UUID.randomUUID());

        assertThrows(ResourceNotFoundException.class,()->concertService.createConcert(request, user)
        );
    }

    @Test

    void getConcertDetailShouldReturnConcert() {

        ConcertResponse response=concertService.getConcertDetail(concert.getId());

        assertEquals(concert.getTitle(), response.getTitle());
    }
    @Test
    void getConcertDetailShouldThrowWhenNotFound() {

        assertThrows(ResourceNotFoundException.class, () -> concertService.getConcertDetail(
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void publishConcertShouldActivateConcert() {
        Concert draftConcert =
                concertRepository.save(
                        Concert.builder()
                                .title("Draft Concert")
                                .artistName("Artist")
                                .description("Desc")
                                .venue(venue)
                                .organizer(organizer)
                                .concertDate(LocalDateTime.now().plusDays(20))
                                .saleStartTime(LocalDateTime.now())
                                .status(ConcertStatus.DRAFT)
                                .category(testCategory)
                                .build()
                );
        TicketTier tier = ticketTierRepository.save(
                        TicketTier.builder()
                                .concert(draftConcert)
                                .section(section)
                                .tierName("GA")
                                .price(BigDecimal.valueOf(1000))
                                .totalQuantity(100)
                                .availableQuantity(100)
                                .tierStatus(TierStatus.UPCOMING)
                                .isActive(true)
                                .maxPerUser(5)
                                .build()
                );

        ConcertResponse response = concertService.publishConcert(draftConcert.getId(), organizer);


        Concert updated = concertRepository.findById(draftConcert.getId()).orElseThrow();

        assertEquals(ConcertStatus.PUBLISHED, updated.getStatus());

        TicketTier updatedTier = ticketTierRepository.findById(tier.getId()).orElseThrow();

        assertEquals(TierStatus.ACTIVE, updatedTier.getTierStatus());
    }
}
