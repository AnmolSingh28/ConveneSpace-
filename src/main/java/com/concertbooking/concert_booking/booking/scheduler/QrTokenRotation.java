package com.concertbooking.concert_booking.booking.scheduler;

import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class QrTokenRotation {
    private final BookingItemRepository bookingItemRepository;

    @Scheduled(fixedRate = 21600000)
    @Transactional
    public void rotateQrTokens(){
        List<BookingItem> items=bookingItemRepository.findConfirmedBookingItems();

        if(items.isEmpty()) return;
        LocalDateTime expiresAt=LocalDateTime.now().plusHours(6);

        items.forEach(item->{
            item.setQrToken(generateToken());
            item.setQrTokenExpiresAt(expiresAt);
        });
        bookingItemRepository.saveAll(items);
        log.debug("Rotated QR tokens for {} items", items.size());
    }
    private String generateToken() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase();
    }
}
