package com.concertbooking.concert_booking.booking.service;

import com.concertbooking.concert_booking.booking.dto.BookingRequest;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.common.enums.SeatStatus;
import com.concertbooking.concert_booking.common.exception.BookingException;
import com.concertbooking.concert_booking.common.exception.SeatUnavailableException;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.seat.entity.SeatInventory;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingItemService {

    private final SeatInventoryRepository seatInventoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TIER_LOCK_PREFIX="tier:user:";

    public List<BookingItem> buildGaItems(BookingRequest request, TicketTier tier, User user) {

        String lockKey=TIER_LOCK_PREFIX + tier.getId() + ":" + user.getId();
        Object lockedQty=redisTemplate.opsForValue().get(lockKey);

        if (lockedQty==null) {throw new BookingException(
                    "No seat lock found. Please select your tickets again.");
        }

        if (Integer.parseInt(lockedQty.toString())!=request.getQuantity()) {
            throw new BookingException(
                    "Locked quantity mismatch. Please select your tickets again.");
        }
        List<BookingItem> items=new ArrayList<>();
        items.add(BookingItem.builder()
                .tier(tier)
                .seatInventory(null)
                .quantity(request.getQuantity())
                .priceAtBooking(tier.getPrice())
                .checkedIn(false)
                .build());
        return items;
    }

    public List<BookingItem> buildAssignedItems(BookingRequest request, TicketTier tier, User user) {

        if (request.getSeatIds()==null || request.getSeatIds().isEmpty()) {
            throw new BookingException("Seat IDs are required for assigned seating");
        }

        List<BookingItem> items=new ArrayList<>();
        List<UUID> sortedSeatIds=request.getSeatIds();
        for (UUID seatId:sortedSeatIds) {
            SeatInventory seat=seatInventoryRepository.findById(seatId)
                    .orElseThrow(()-> new SeatUnavailableException(
                            "Seat not found: " + seatId));

            validateSeatLock(seat, user);
            items.add(BookingItem.builder()
                    .tier(tier)
                    .seatInventory(seat)
                    .quantity(1)
                    .priceAtBooking(tier.getPrice())
                    .checkedIn(false)
                    .build());
        }

        return items;
    }

    private void validateSeatLock(SeatInventory seat, User user) {
        if (seat.getStatus() != SeatStatus.LOCKED) {
            throw new SeatUnavailableException("Seat " + seat.getRowLabel() + seat.getSeatNumber() +
                            " is no longer locked. Please select again.");
        }
        if (!seat.getLockedByUser().getId().equals(user.getId())) {
            throw new SeatUnavailableException(
                    "Seat " + seat.getRowLabel() + seat.getSeatNumber() +
                            " is locked by another user.");
        }
        if (seat.getLockedUntil().isBefore(LocalDateTime.now())) {
            throw new SeatUnavailableException(
                    "Seat lock expired for " + seat.getRowLabel() +
                            seat.getSeatNumber() + ". Please select again.");
        }
    }
}
