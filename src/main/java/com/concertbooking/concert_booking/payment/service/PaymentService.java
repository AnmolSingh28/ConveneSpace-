package com.concertbooking.concert_booking.payment.service;

import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.event.BookingCancelledEvent;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.common.enums.BookingStatus;
import com.concertbooking.concert_booking.common.enums.PaymentStatus;
import com.concertbooking.concert_booking.common.enums.SeatStatus;
import com.concertbooking.concert_booking.common.exception.PaymentException;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.metrics.MetricsService;
import com.concertbooking.concert_booking.notification.service.NotificationPublisher;
import com.concertbooking.concert_booking.payment.dto.CreatePaymentRequest;
import com.concertbooking.concert_booking.payment.dto.PaymentResponse;
import com.concertbooking.concert_booking.seat.service.SeatLockService;
import com.concertbooking.concert_booking.payment.entity.Payment;
import com.concertbooking.concert_booking.payment.event.PaymentSuccessEvent;
import com.concertbooking.concert_booking.payment.mapper.PaymentMapper;
import com.concertbooking.concert_booking.payment.repository.PaymentRepository;
import com.concertbooking.concert_booking.seat.entity.SeatInventory;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;




@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final TicketTierRepository ticketTierRepository;
    private final PaymentMapper paymentMapper;
    private final MetricsService metricsService;
    private final NotificationPublisher notificationPublisher;
    private final SeatLockService seatLockService;
    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency}")
    private String currency;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;



    //CREATE ORDER
    @Transactional
    public PaymentResponse createOrder(CreatePaymentRequest request,UUID userId) {
        //loading booking
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found: " + request.getBookingId()
                ));
        //current booking must belong to user
        if (!booking.getUser().getId().equals(userId)) {
            throw new PaymentException("Access Denied");
        }
        //Bookings which are pending==in payable state
        if(booking.getStatus()!= BookingStatus.PENDING){
            throw new PaymentException("Booking is not in payable state: "+ booking.getStatus());
        }
        //if payment is already done or exists
        if(paymentRepository.existsByBookingIdAndStatus(booking.getId(), PaymentStatus.SUCCESS)){
            throw new PaymentException("Booking is already paid");
        }

        Payment payment=paymentRepository
                .findByBookingId(booking.getId())
                .orElse(Payment.builder()
                        .booking(booking)
                        .amount(booking.getTotalAmount())
                        .status(PaymentStatus.INITIATED)
                        .attemptCount(0)
                        .build()
                );
        //increment attempt count for idempotency check
        payment.setAttemptCount(payment.getAttemptCount()+1);
        payment.setStatus(PaymentStatus.INITIATED);

        try{
            RazorpayClient client = new RazorpayClient(
                    razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();

            orderRequest.put("amount", booking.getTotalAmount()
                            .multiply(BigDecimal.valueOf(100))
                            .intValue());
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", booking.getBookingReference());
            orderRequest.put("payment_capture", 1);

            Order order=client.orders.create(orderRequest);
            String razorpayOrderId = order.get("id");

            payment.setRazorpayOrderId(razorpayOrderId);
            Payment saved=paymentRepository.save(payment);

            log.info("Razorpay order created: {} for booking: {}",
                    razorpayOrderId, booking.getBookingReference());

            return new PaymentResponse(
                    saved.getId(),
                    booking.getId(),
                    booking.getBookingReference(),
                    saved.getAmount(),
                    saved.getStatus(),
                    saved.getRazorpayOrderId(),
                    null,
                    razorpayKeyId,
                    currency,
                    saved.getPaymentMethod(),
                    saved.getRefundAmount(),
                    saved.getRefundedAt(),
                    saved.getFailureReason(),
                    saved.getAttemptCount(),
                    saved.getCreatedAt()
            );

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}",e.getMessage());
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);
            throw new PaymentException("Payment gateway error: " + e.getMessage());
        }
    }
    // WEBHOOK-
    @Transactional
    public void handleWebhook(String payload,String signature){
        //match the signature ,if sign doesnt match and reject it
        log.info("Webhook hit signature: {}",signature);
        log.info("Webhook payload: {}", payload);
        if(!verifyWebhookSignature(payload,signature)){
            log.warn("Invalid webhook signature");
            throw new PaymentException("Invalid webhook signature");
        }
        JSONObject event=new JSONObject(payload);
        String evenType=event.getString("event");
        log.info("Webhook received: {}",evenType);
        switch (evenType){
            case "payment.captured"-> handlePaymentSuccess(event);
            case "payment.failed"-> handlePaymentFailed(event);
            case "refund.processed"-> handleRefundProcessed(event);
            case "payment.pending" -> handlePaymentPending(event);
            default-> log.info("Unhandled webhook event detected: {}",evenType);
        }
    }
    // PAYMENT SUCCESS-
    private void handlePaymentSuccess(JSONObject event){
        // nested JSON
        JSONObject paymentData=event.getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");
        // extract  the below from the webhook
        String razorpayPaymentId=paymentData.getString("id");
        String razorpayOrderId=paymentData.getString("order_id");
        String paymentMethod=paymentData.getString("method");

        Payment payment=paymentRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(()->new ResourceNotFoundException("There is no payment for this order: "+razorpayOrderId));

        // got the payment now check its status if its successful
        if(payment.getStatus()==PaymentStatus.SUCCESS){
            log.info("Payment already processed: {}",razorpayPaymentId);
            return;
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setPaymentMethod(paymentMethod);
        paymentRepository.save(payment);
        metricsService.addRevenue(payment.getAmount());
        metricsService.incrementSuccessfulPayments();

        //Confirm booking
        Booking booking=payment.getBooking();
        int totalTickets = booking.getItems()
                .stream()
                .mapToInt(BookingItem::getQuantity)
                .sum();

        metricsService.addTicketsSold(totalTickets);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        //confirm seats
        confirmSeats(booking);
        for (BookingItem item : booking.getItems()) {
            // GA tier
            if (item.getSeatInventory() == null) {
                int updated = ticketTierRepository.decrementAvailableQuantity(item.getTier().getId(), item.getQuantity());
                if (updated == 0) {
                    throw new PaymentException("Tickets no longer available during payment confirmation"
                    );
                }
                //remove temporary Redis reservation
                seatLockService.consumeGaLockOnPaymentSuccess(
                        item.getTier().getId(),
                        booking.getUser()
                );
            }
        }
        log.info("Payment successful and your booking is confirmed: {}",
                booking.getBookingReference());
        PaymentSuccessEvent successEvent = PaymentSuccessEvent.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUser().getId())
                .userEmail(booking.getUser().getEmail())
                .userName(booking.getUser().getName())
                .concertTitle(booking.getConcert().getTitle())
                .amount(payment.getAmount())
                .paymentMethod(paymentMethod)
                .paidAt(LocalDateTime.now())
                .organizerId(booking.getConcert().getOrganizer().getId())
                .organizerName(booking.getConcert().getOrganizer().getName())
                .build();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationPublisher.publishBookingConfirmation(successEvent);
                        notificationPublisher.publishQrGeneration(successEvent);
                        notificationPublisher.publishAnalyticsUpdate(successEvent);
                        log.info("Payment events published after commit for: {}",
                                booking.getBookingReference());
                    }
                }
        );

    }
    // PAYMENT FAILED-
    private void handlePaymentFailed(JSONObject event){
        JSONObject paymentData=event.getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        String razorpayOrderId=paymentData.getString("order_id");
        String failureReason=paymentData.optString("error_description","Payment Failed");
        Payment payment=paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(()->new ResourceNotFoundException("Payment not found for this order: {}"+razorpayOrderId));

        if(payment.getStatus()==PaymentStatus.FAILED){
            return;
        }
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        paymentRepository.save(payment);
        metricsService.incrementFailedPayments();

        Booking booking=payment.getBooking();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason("Payment failed: "+failureReason);
        bookingRepository.save(booking);
        metricsService.incrementBookingCancellations();

        //Release the seats
        releaseSeats(booking);

        log.info("Payment failed seats are released for booking: {}"+booking.getBookingReference());
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notificationPublisher.publishBookingCancelled(
                                BookingCancelledEvent.builder()
                                        .bookingId(booking.getId())
                                        .bookingReference(booking.getBookingReference())
                                        .userId(booking.getUser().getId())
                                        .userEmail(booking.getUser().getEmail())
                                        .userName(booking.getUser().getName())
                                        .concertTitle(booking.getConcert().getTitle())
                                        .refundAmount(BigDecimal.ZERO)
                                        .cancellationReason(booking.getCancellationReason())
                                        .cancelledAt(booking.getCancelledAt())
                                        .build()
                        );
                    }
                }
        );


    }

    private void handleRefundProcessed(JSONObject event) {
        JSONObject refundData = event.getJSONObject("payload")
                .getJSONObject("refund")
                .getJSONObject("entity");

        String razorpayPaymentId = refundData.getString("payment_id");
        String razorpayRefundId = refundData.getString("id");
        int refundAmountPaise = refundData.getInt("amount");

        //convert razorpay's paise amount to proper rs
        BigDecimal amount = BigDecimal.valueOf(refundAmountPaise).divide(BigDecimal.valueOf(100));

        Payment payment = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + razorpayPaymentId));

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRazorpayRefundId(razorpayRefundId);
        payment.setRefundAmount(amount);
        payment.setRefundedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("Refund processed: {} Amount: ₹{}", razorpayRefundId, amount);
    }
        //get payment by booking
        public PaymentResponse getPaymentByBooking(UUID bookingId,UUID userId) {
            Booking booking=bookingRepository.findById(bookingId)
                    .orElseThrow(()->new ResourceNotFoundException("Booking not found: " +bookingId));

            if (!booking.getUser().getId().equals(userId)) {
                throw new PaymentException("Access denied");
            }
            Payment payment=paymentRepository.findByBookingId(bookingId)
                    .orElseThrow(()->new ResourceNotFoundException(
                            "Payment not found for booking: " + bookingId));

            return paymentMapper.toResponse(payment);
        }
        // CONFIRM seats-
    private void confirmSeats(Booking booking){
        for(BookingItem item: booking.getItems()){
            if(item.getSeatInventory()!=null){
                SeatInventory seat=item.getSeatInventory();
                seat.setStatus(SeatStatus.BOOKED);
                seat.setLockedByUser(null);
                seat.setLockedUntil(null);
                seatInventoryRepository.save(seat);
            }
        }
    }
    //RELEASE SEATS-
    private void releaseSeats(Booking booking){
        for(BookingItem item: booking.getItems()){
            if(item.getSeatInventory()!=null){
                SeatInventory seat=item.getSeatInventory();
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setLockedByUser(null);
                seat.setLockedUntil(null);
                seatInventoryRepository.save(seat);
            }else{

                seatLockService.releaseGaLock(
                        item.getTier().getId(),
                        booking.getUser()
                );
            }
        }
    }
    //WEBHOOK SIGNATURE VALIDATION
    private boolean verifyWebhookSignature(String payload,String signature) {
        try{
            Mac mac=Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey=new SecretKeySpec(
                    webhookSecret.getBytes(),"HmacSHA256");
            mac.init(secretKey);
            byte[] hash=mac.doFinal(payload.getBytes());
            String computed=HexFormat.of().formatHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private void handlePaymentPending(JSONObject event) {
        JSONObject paymentData = event.getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        String razorpayOrderId = paymentData.getString("order_id");

        Payment payment = paymentRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for order: " + razorpayOrderId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) return;

        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        log.info("Payment pending — seat hold maintained for booking: {}",
                payment.getBooking().getBookingReference());
    }
    @Transactional
    public void simulatePaymentSuccess(UUID bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found: " + bookingId));

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for booking: " + bookingId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            log.warn("Simulate called on non-pending booking: {} status: {}",bookingId,booking.getStatus());
            return;
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRazorpayPaymentId("LOAD_TEST_" + UUID.randomUUID());
        payment.setPaymentMethod("LOAD_TEST");
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        for (BookingItem item : booking.getItems()) {
            if (item.getSeatInventory() == null) { // GA tier
                int updated = ticketTierRepository.decrementAvailableQuantity(
                        item.getTier().getId(), item.getQuantity());

                if (updated == 0) {
                    log.warn("LOAD TEST: Quantity exhausted for tier: {} booking: {}",
                            item.getTier().getId(), bookingId);
                    booking.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(booking);
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                    return;
                }

                seatLockService.consumeGaLockOnPaymentSuccess(
                        item.getTier().getId(),
                        booking.getUser());
            }
        }

        log.info("LOAD TEST PAYMENT SUCCESS -> {}", booking.getBookingReference());
    }
    @Transactional
    public void cancelPayment(UUID bookingId) {

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        releaseSeats(booking);
    }

    }



