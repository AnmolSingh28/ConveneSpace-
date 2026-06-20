package com.concertbooking.concert_booking.integration;

import com.concertbooking.concert_booking.booking.dto.BookingRequest;
import com.concertbooking.concert_booking.booking.dto.BookingResponse;
import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.booking.service.BookingService;
import com.concertbooking.concert_booking.common.enums.*;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.ConcertRepository;
import com.concertbooking.concert_booking.concert.repository.EventCategoryRepository;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.payment.dto.CreatePaymentRequest;
import com.concertbooking.concert_booking.payment.dto.PaymentResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest

@ActiveProfiles("test")
public class EndToEndBookingFlowIntegrationTest {
    @Autowired private BookingService bookingService;
    @Autowired private PaymentService paymentService;
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private VenueSectionRepository venueSectionRepository;
    @Autowired private ConcertRepository concertRepository;
    @Autowired private TicketTierRepository ticketTierRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private BookingItemRepository bookingItemRepository;
    @Autowired
    private EventCategoryRepository eventCategoryRepository;
    @Autowired private SeatLockService seatLockService;
    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;
    @PersistenceContext
    private EntityManager entityManager;

    private User user;
    private TicketTier tier;
    private Concert concert;
    private Booking booking;
    private Payment payment;
    private Venue venue;
    private VenueSection section;
    private BookingItem bookingItem;
    @BeforeEach
    void setup() {
        bookingItemRepository.deleteAll();
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        ticketTierRepository.deleteAll();
        concertRepository.deleteAll();
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
        EventCategory testCategory = eventCategoryRepository.save(
                EventCategory.builder()
                        .name("Live Music")
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
    void paymentFailureShouldCancelBookingFlow() throws Exception {


        seatLockService.lockGaTier(tier.getId(),2,user);


        BookingRequest request = new BookingRequest();

        request.setTierId(tier.getId());
        request.setQuantity(2);
        request.setIdempotencyKey(UUID.randomUUID().toString());

        BookingResponse bookingResponse=bookingService.createBooking(request, user);

        Booking booking =bookingRepository.findById(bookingResponse.id()).orElseThrow();

        assertEquals(BookingStatus.PENDING, booking.getStatus());

        CreatePaymentRequest paymentRequest=new CreatePaymentRequest();

        paymentRequest.setBookingId(booking.getId());

        PaymentResponse paymentResponse = paymentService.createOrder(paymentRequest,
                        user.getId());

        Payment payment = paymentRepository.findById(paymentResponse.id()).orElseThrow();

        assertEquals(PaymentStatus.INITIATED, payment.getStatus());


        String payload = """
    {
      "event":"payment.failed",
      "payload":{
        "payment":{
          "entity":{
            "order_id":"%s",
            "error_description":"Card declined"
          }
        }
      }
    }
    """.formatted(paymentResponse.razorpayOrderId());

        String signature = generateSignature(payload);

        paymentService.handleWebhook(payload, signature);


        Payment updatedPayment=paymentRepository.findById(payment.getId()
                ).orElseThrow();

        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        TicketTier updatedTier = ticketTierRepository.findById(tier.getId()).orElseThrow();

        assertEquals(PaymentStatus.FAILED, updatedPayment.getStatus());

        assertEquals(BookingStatus.CANCELLED, updatedBooking.getStatus());

        assertNotNull(updatedBooking.getCancelledAt());

        assertTrue(updatedBooking.getCancellationReason().contains("Card declined"));

        assertEquals(100, updatedTier.getAvailableQuantity());
    }
    @Test
    void successfulBookingJourney() throws Exception {


        seatLockService.lockGaTier(tier.getId(), 2, user);


        BookingRequest request = new BookingRequest();

        request.setTierId(tier.getId());
        request.setQuantity(2);
        request.setIdempotencyKey(UUID.randomUUID().toString());

        BookingResponse bookingResponse = bookingService.createBooking(request, user);

        Booking booking=bookingRepository.findById(bookingResponse.id()).orElseThrow();

        assertEquals(BookingStatus.PENDING,booking.getStatus());



        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();

        paymentRequest.setBookingId(booking.getId());

        PaymentResponse paymentResponse = paymentService.createOrder(paymentRequest, user.getId());

        Payment payment = paymentRepository.findById(paymentResponse.id()).orElseThrow();

        assertEquals(PaymentStatus.INITIATED, payment.getStatus());


        String paymentId = "pay-" + UUID.randomUUID();
        String payload = """
        {
          "event":"payment.captured",
          "payload":{
            "payment":{
              "entity":{
                "id":"%s",
                "order_id":"%s",
                "method":"upi"
              }
            }
          }
        }
        """.formatted(paymentId,paymentResponse.razorpayOrderId()
        );

        String signature = generateSignature(payload);

        paymentService.handleWebhook(payload, signature);

        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();

        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        TicketTier updatedTier = ticketTierRepository.findById(tier.getId()).orElseThrow();

        assertEquals(PaymentStatus.SUCCESS, updatedPayment.getStatus());

        assertEquals(BookingStatus.CONFIRMED, updatedBooking.getStatus());

        assertEquals(98, updatedTier.getAvailableQuantity());
    }
    private String generateSignature(String payload) throws Exception{
        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");

        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes());
        return HexFormat.of().formatHex(hash);
    }
}
