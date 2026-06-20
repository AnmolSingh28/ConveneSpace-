package com.concertbooking.concert_booking.config;

import com.concertbooking.concert_booking.common.enums.SeatStatus;
import com.concertbooking.concert_booking.seat.entity.SeatInventory;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.seat.service.SeatLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketDisconnectHandler  implements   ApplicationListener<SessionDisconnectEvent> {

    private final SeatInventoryRepository seatInventoryRepository;
    private final SeatLockService seatLockService;

    @Override
    @Transactional
    public void onApplicationEvent(SessionDisconnectEvent event){
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Principal user = accessor.getUser();
        if (user==null) return;

        UUID userId = UUID.fromString(user.getName());
        List<SeatInventory> lockedSeats = seatInventoryRepository.findByLockedByUserIdAndStatus(userId, SeatStatus.LOCKED);

        for (SeatInventory seat:lockedSeats){
            seatLockService.releaseAssignedSeat(seat.getId());
            log.info("Seat released on WS disconnect: {} user: {}", seat.getId(),userId);
        }
    }
}
