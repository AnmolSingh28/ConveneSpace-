package com.concertbooking.concert_booking.booking.service;
import com.concertbooking.concert_booking.metrics.MetricsService;
import com.concertbooking.concert_booking.booking.dto.BookingRequest;
import com.concertbooking.concert_booking.booking.dto.BookingResponse;
import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.event.BookingCancelledEvent;
import com.concertbooking.concert_booking.booking.mapper.BookingMapper;
import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.common.enums.BookingStatus;
import com.concertbooking.concert_booking.common.enums.SeatStatus;
import com.concertbooking.concert_booking.common.enums.SectionType;
import com.concertbooking.concert_booking.common.exception.BookingException;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.notification.service.NotificationPublisher;
import com.concertbooking.concert_booking.queue.service.VirtualQueueService;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.user.entity.User;
import com.concertbooking.concert_booking.seat.service.SeatLockService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final TicketTierRepository ticketTierRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final BookingValidationService bookingValidationService;
    private final BookingPricingService bookingPricingService;
    private final BookingItemService bookingItemService;
    private final BookingMapper bookingMapper;
    private final VirtualQueueService virtualQueueService;
    private final RedisTemplate<String,Object>redisTemplate;
    private final NotificationPublisher notificationPublisher;
    private final SeatLockService seatLockService;
    private final MetricsService metricsService;

    // CREATE
    @Transactional
    public BookingResponse createBooking(BookingRequest request, User user){
        // Idempotency check
        if (bookingRepository.existsByIdempotencyKey(request.getIdempotencyKey())){
            return bookingMapper.toResponse(
                    bookingRepository.findByIdempotencyKey(request.getIdempotencyKey())
                            .orElseThrow()
            );
        }
        // Loads tier
        TicketTier tier = ticketTierRepository.findById(request.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket tier not found: " + request.getTierId()));
        if (virtualQueueService.isQueueActive(tier.getId())){
            // User already has a lock

            String userLockKey = "tier:user:" + tier.getId() + ":" + user.getId();
            boolean hasLock=Boolean.TRUE.equals(redisTemplate.hasKey(userLockKey));

            if (!hasLock && !virtualQueueService.hasValidQueueToken(tier.getId(), user)){
                throw new BookingException("Please wait in queue before booking");
            }

       /*     if (virtualQueueService.hasValidQueueToken(tier.getId(), user)) {
                virtualQueueService.consumeToken(tier.getId(), user);
            }*/
        }
        bookingValidationService.validateBookingRequest(request,tier,user);

        //Build items
        boolean isGa=tier.getSection().getSectionType()==SectionType.GA;
        List<BookingItem> items = isGa
                ? bookingItemService.buildGaItems(request, tier, user)
                : bookingItemService.buildAssignedItems(request, tier, user);

        //Pricing
        BigDecimal baseAmount = tier.getPrice().multiply(BigDecimal.valueOf(isGa ? request.getQuantity() : items.size()));
        BigDecimal platformFee = bookingPricingService.calculatePlatformFee(baseAmount);
        BigDecimal gatewayFee = bookingPricingService.calculateGatewayFee(baseAmount);
        BigDecimal totalAmount = bookingPricingService.calculateTotal(baseAmount, platformFee, gatewayFee);

        //Persist/save
        Booking booking = Booking.builder()
                .bookingReference(generateReference())
                .user(user)
                .concert(tier.getConcert())
                .status(BookingStatus.PENDING)
                .baseAmount(baseAmount)
                .platformFee(platformFee)
                .paymentGatewayFee(gatewayFee)
                .totalAmount(totalAmount)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        Booking saved = bookingRepository.save(booking);
        items.forEach(item -> item.setBooking(saved));
        bookingItemRepository.saveAll(items);
        saved.setItems(items);

        log.info("Booking created: {} user: {} concert: {}",
                saved.getBookingReference(),
                user.getEmail(),
                tier.getConcert().getTitle());

        if(virtualQueueService.hasValidQueueToken(tier.getId(),user)
        ){ virtualQueueService.consumeToken(tier.getId(), user);
            virtualQueueService.removeFromActiveAdmissions(tier.getId(), user.getId());
        }
        metricsService.incrementBookings();
        return bookingMapper.toResponse(saved);
    }

        // CANCEL
        @Transactional
        public BookingResponse cancelBooking(UUID bookingId,User user,String reason){
            Booking booking=bookingRepository.findById(bookingId)
                    .orElseThrow(()->new ResourceNotFoundException("Booking not found: "+bookingId));
            if(!booking.getUser().getId().equals(user.getId())) {
                throw new BookingException(
                        "You are not authorized to cancel this booking");
            }
            if(booking.getStatus()!=BookingStatus.PENDING
                    && booking.getStatus()!=BookingStatus.CONFIRMED) {
                throw new BookingException("Booking cannot be cancelled. Status: " + booking.getStatus());
            }

            BigDecimal refund=bookingPricingService.calculateRefund(booking);
            releaseSeats(booking);

            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(LocalDateTime.now());
            booking.setCancellationReason(reason);
            booking.setRefundAmount(refund);
            bookingRepository.save(booking);
            metricsService.incrementBookingCancellations();

            log.info("Booking cancelled: {} refund: {}",booking.getBookingReference(),refund);

            BookingCancelledEvent cancelledEvent=BookingCancelledEvent.builder()
                    .bookingId(booking.getId())
                    .bookingReference(booking.getBookingReference())
                    .userId(user.getId())
                    .userEmail(user.getEmail())
                    .userName(user.getName())
                    .concertTitle(booking.getConcert().getTitle())
                    .refundAmount(refund)
                    .cancellationReason(reason)
                    .cancelledAt(booking.getCancelledAt())
                    .build();


            notificationPublisher.publishBookingCancelled(cancelledEvent);
            
            return bookingMapper.toResponse(booking);
        }


    public Page<BookingResponse> getUserBookings(User user, Pageable pageable){
        return bookingRepository
                .findByUserId(user.getId(),pageable)
                .map(bookingMapper::toResponse);

    }
    public BookingResponse getBookingByReference(String reference,User user){
        Booking booking=bookingRepository
                .findByBookingReference(reference)
                .orElseThrow(()->new ResourceNotFoundException(
                        "Booking not found: " + reference));

        if (!booking.getUser().getId().equals(user.getId())){
            throw new BookingException("Access denied");
        }
        return bookingMapper.toResponse(booking);
    }
    public BookingResponse getBookingById(UUID bookingId,User user){
        Booking booking= bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if(!booking.getUser().getId().equals(user.getId())){
            throw new BookingException("Access Denied");
        }
        return bookingMapper.toResponse(booking);
    }

  //HELPERS


    private void releaseSeats(Booking booking){
        for(BookingItem item:booking.getItems()){
            if(item.getSeatInventory()!=null){
                var seat=item.getSeatInventory();
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setLockedByUser(null);
                seat.setLockedUntil(null);
                seatInventoryRepository.save(seat);
            }else{
                seatLockService.releaseGaLock(item.getTier().getId(),booking.getUser());
            }
        }
    }
    private String generateReference(){
        return "CB-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }
    public Map<String, Object> getEstimate(UUID tierId, int quantity){
        TicketTier tier = ticketTierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found"));

        BigDecimal baseAmount = tier.getPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal platformFee = bookingPricingService.calculatePlatformFee(baseAmount);
        BigDecimal gatewayFee = bookingPricingService.calculateGatewayFee(baseAmount);
        BigDecimal total = bookingPricingService.calculateTotal(baseAmount, platformFee, gatewayFee);

        return Map.of(
                "baseAmount", baseAmount,
                "platformFee", platformFee,
                "gatewayFee", gatewayFee,
                "totalAmount", total,
                "quantity", quantity
        );
    }
    public Map<String, Object> canBook(UUID tierId, User user){
        TicketTier tier = ticketTierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found"));

        int lockedCount = seatInventoryRepository.countByLockedByUserIdAndConcertId(user.getId(), tier.getConcert().getId());

        int confirmedCount = bookingItemRepository.countConfirmedSeatsForUserAndConcert(user.getId(), tier.getConcert().getId());

        int remaining = tier.getMaxPerUser() - confirmedCount;

        return Map.of(
                "canBook", remaining > 0,
                "remainingSlots", Math.max(0, remaining),
                "maxPerUser", tier.getMaxPerUser(),
                "reason", remaining > 0 ? "OK" : "Max ticket limit reached"
        );
    }
 

}
