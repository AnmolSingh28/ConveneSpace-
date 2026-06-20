package com.concertbooking.concert_booking.booking.scheduler;

import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.common.enums.BookingStatus;
import com.concertbooking.concert_booking.common.enums.SeatStatus;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class PendingBookingCleanupScheduler {
    private final BookingRepository bookingRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private static final String TIER_LOCK_PREFIX="tier:lock:";

    @Value("${app.booking.pending-expiry-minutes:2}")
    private int pendingExpiryMinutes;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void cleanupExpiredPendingBookings(){
        LocalDateTime cutoff=LocalDateTime.now().minusMinutes(pendingExpiryMinutes);

        List<Booking> expired=bookingRepository.findExpiredPendingBookings(cutoff);
        if(expired.isEmpty()){log.debug("No expired pending bookings found");
            return;
        }

        log.info("Found {} expired pending bookings to cleanup",expired.size());
        int cleaned = 0;
        for (Booking booking : expired){
            try {
                cancelExpiredBooking(booking);
                cleaned++;
            } catch (Exception e){
                log.error("Failed to cleanup booking: {} — {}",booking.getBookingReference(),e.getMessage());
            }
        }
        log.info("Cleanup complete — cancelled {} expired pending bookings",cleaned);
    }
    private void cancelExpiredBooking(Booking booking){

        for (BookingItem item: booking.getItems()){
            if(item.getSeatInventory()!=null){

                var seat = item.getSeatInventory();
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setLockedByUser(null);
                seat.setLockedUntil(null);
                seatInventoryRepository.save(seat);
                simpMessagingTemplate.convertAndSend(
                        "/topic/concert/" + item.getTier().getConcert().getId() + "/seats",
                        Map.of(
                                "seatId", seat.getId(),
                                "status", "AVAILABLE",
                                "timestamp", System.currentTimeMillis()
                        )
                );
            }else{
                String redisKey=TIER_LOCK_PREFIX +item.getTier().getId() + ":" + booking.getUser().getId();
                redisTemplate.delete(redisKey);
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason("Auto-cancelled: payment not completed within" + pendingExpiryMinutes + "minutes");
        bookingRepository.save(booking);

        log.info("Auto-cancelled booking: {} (created: {})",booking.getBookingReference(), booking.getCreatedAt());
    }
}
