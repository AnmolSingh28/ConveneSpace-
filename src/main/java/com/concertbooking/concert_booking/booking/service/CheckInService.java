package com.concertbooking.concert_booking.booking.service;

import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.common.enums.BookingStatus;
import com.concertbooking.concert_booking.common.exception.BookingException;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.metrics.MetricsService;
import com.concertbooking.concert_booking.notification.service.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInService {
    private final BookingItemRepository bookingItemRepository;
    private final BookingRepository bookingRepository;
    private final NotificationPublisher notificationPublisher;
    private final MetricsService metricsService;
    @Transactional
    public String checkIn(String qrToken) {
        BookingItem item = bookingItemRepository.findByQrToken(qrToken).orElseThrow(() -> new ResourceNotFoundException("Invalid QR code"));

        if (item.getQrTokenExpiresAt() != null&&item.getQrTokenExpiresAt().isBefore(LocalDateTime.now())){
            throw new BookingException("QR code has expired. Ask user to refresh their ticket.");
        }

        if (item.getBooking().getStatus() != BookingStatus.CONFIRMED && item.getBooking().getStatus() != BookingStatus.ATTENDED){
            throw new BookingException("Booking is not confirmed. Status: "+item.getBooking().getStatus());
        }
        if (item.isCheckedIn()) {
            throw new BookingException("Ticket already used. Checked in at: " + item.getCheckedInAt());
        }

        item.setCheckedIn(true);
        item.setCheckedInAt(LocalDateTime.now());
        bookingItemRepository.save(item);
        metricsService.incrementCheckins();

        Booking booking = item.getBooking();

        boolean allCheckedIn = booking.getItems().stream().allMatch(BookingItem::isCheckedIn);
        if (allCheckedIn){
            booking.setStatus(BookingStatus.ATTENDED);
            bookingRepository.save(booking);
        }
        notificationPublisher.publishCheckinEvent(booking.getConcert().getOrganizer().getId());
        String bookingRef = booking.getBookingReference();
        String userName = booking.getUser().getName();
        String tierName = item.getTier().getTierName();

        log.info("Check-in successful — booking: {}, item: {}, user: {}", bookingRef,item.getId(),userName);

        return String.format("Welcome %s! %s — %s ✓", userName,bookingRef,tierName);
    }
}
