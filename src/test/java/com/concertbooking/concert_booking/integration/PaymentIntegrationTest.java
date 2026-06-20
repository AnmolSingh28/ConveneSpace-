package com.concertbooking.concert_booking.integration;

import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.common.enums.*;
import com.concertbooking.concert_booking.common.exception.PaymentException;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.ConcertRepository;
import com.concertbooking.concert_booking.concert.repository.EventCategoryRepository;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.notification.service.NotificationPublisher;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
public class PaymentIntegrationTest {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingItemRepository bookingItemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventCategoryRepository eventCategoryRepository;


    @Autowired
    private ConcertRepository concertRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private VenueSectionRepository venueSectionRepository;
    @Autowired
    private TicketTierRepository ticketTierRepository;

    @MockBean
    private NotificationPublisher notificationPublisher;

    @MockBean
    private SeatLockService seatLockService;

    private User user;
    private Concert concert;
    private TicketTier tier;
    private Booking booking;
    private Payment payment;
    private Venue venue;
    private VenueSection section;
    private BookingItem bookingItem;
    private EventCategory testCategory;
    @BeforeEach
    void setup() {

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
                        .name("Payment Test Category " + UUID.randomUUID())
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

        String orderId="order-"+UUID.randomUUID();
        payment=paymentRepository.save(
                Payment.builder()
                        .booking(booking)
                        .amount(BigDecimal.valueOf(2000))
                        .status(PaymentStatus.PENDING)
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
    private String generateSignature(String payload) throws Exception {
        String secret="hello28@";
        Mac mac=Mac.getInstance("HmacSHA256");
        SecretKeySpec key=new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(key);
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes()));
    }
    @Test
    void paymentCapturedShouldConfirmBookingAndReduceInventory() throws Exception {

        int before=ticketTierRepository.findById(tier.getId()).orElseThrow().getAvailableQuantity();
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
""".formatted(paymentId,payment.getRazorpayOrderId());

        String signature=generateSignature(payload);
        paymentService.handleWebhook(payload, signature);

        Payment updatedPayment=paymentRepository.findById(payment.getId()).orElseThrow();

        Booking updatedBooking=bookingRepository.findById(booking.getId()).orElseThrow();

        TicketTier updatedTier=ticketTierRepository.findById(tier.getId()).orElseThrow();

        assertEquals(PaymentStatus.SUCCESS, updatedPayment.getStatus());

        assertEquals(BookingStatus.CONFIRMED, updatedBooking.getStatus());

        assertEquals(before - 2, updatedTier.getAvailableQuantity());
    }
    @Test
    void duplicatePaymentCapturedWebhookShouldNotReduceInventoryTwice() throws Exception {
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
""".formatted(paymentId,payment.getRazorpayOrderId());

        String signature = generateSignature(payload);

        paymentService.handleWebhook(payload,signature);

        paymentService.handleWebhook(payload, signature);


        Payment updatedPayment=paymentRepository.findById(payment.getId()).orElseThrow();

        Booking updatedBooking=bookingRepository.findById(booking.getId()).orElseThrow();

        TicketTier updatedTier=ticketTierRepository.findById(tier.getId()).orElseThrow();

        assertEquals(PaymentStatus.SUCCESS, updatedPayment.getStatus());

        assertEquals(BookingStatus.CONFIRMED, updatedBooking.getStatus());

        assertEquals(98, updatedTier.getAvailableQuantity());
    }

    @Test
    void invalidSignatureShouldRejectWebhook() {
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
""".formatted(paymentId,payment.getRazorpayOrderId());

        PaymentException ex = assertThrows(
                PaymentException.class,
                () -> paymentService.handleWebhook(
                        payload,
                        "invalid_signature"
                )
        );

        assertEquals("Invalid webhook signature", ex.getMessage());


        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();

        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

        TicketTier updatedTier = ticketTierRepository.findById(tier.getId()).orElseThrow();

        assertEquals(PaymentStatus.PENDING, updatedPayment.getStatus());

        assertEquals(BookingStatus.PENDING, updatedBooking.getStatus());

        assertEquals(
                100,
                updatedTier.getAvailableQuantity()
        );
    }
    @Test
    void paymentFailedShouldCancelBookingAndReleaseInventory() throws Exception {
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
""".formatted(payment.getRazorpayOrderId());

        String signature=generateSignature(payload);

        paymentService.handleWebhook(payload,signature);



        Payment updatedPayment=paymentRepository.findById(payment.getId()).orElseThrow();

        Booking updatedBooking=bookingRepository.findById(booking.getId()).orElseThrow();

        assertEquals(PaymentStatus.FAILED, updatedPayment.getStatus());

        assertEquals("Card declined", updatedPayment.getFailureReason());

        assertEquals(BookingStatus.CANCELLED, updatedBooking.getStatus());

        assertNotNull(updatedBooking.getCancelledAt());

        assertTrue(updatedBooking.getCancellationReason().contains("Card declined"));
    }
    @Test
    void paymentPendingShouldKeepBookingPending() throws Exception {
        String paymentId = "pay-" + UUID.randomUUID();
        String payload = """
{
  "event":"payment.pending",
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
""".formatted(paymentId,payment.getRazorpayOrderId());

        String signature = generateSignature(payload);

        paymentService.handleWebhook(payload, signature);


        Payment updatedPayment=paymentRepository.findById(payment.getId()).orElseThrow();

        Booking updatedBooking=bookingRepository.findById(booking.getId()).orElseThrow();

        TicketTier updatedTier=ticketTierRepository.findById(tier.getId()).orElseThrow();

        assertEquals(PaymentStatus.PENDING,updatedPayment.getStatus());

        assertEquals(BookingStatus.PENDING,updatedBooking.getStatus());

        assertEquals(100, updatedTier.getAvailableQuantity());

        assertNull(updatedBooking.getCancelledAt());
    }
}
