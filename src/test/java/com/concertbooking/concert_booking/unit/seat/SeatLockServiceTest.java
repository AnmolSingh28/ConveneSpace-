package com.concertbooking.concert_booking.unit.seat;
import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import com.concertbooking.concert_booking.common.enums.SeatStatus;
import com.concertbooking.concert_booking.common.exception.SeatUnavailableException;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.metrics.MetricsService;
import com.concertbooking.concert_booking.queue.service.VirtualQueueService;
import com.concertbooking.concert_booking.seat.entity.SeatInventory;
import com.concertbooking.concert_booking.seat.mapper.SeatMapper;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.seat.service.SeatLockService;
import com.concertbooking.concert_booking.user.entity.User;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatLockServiceTest {


    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private TicketTierRepository ticketTierRepository;

    @Mock
    private SeatInventoryRepository seatInventoryRepository;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private SeatMapper seatMapper;

    @Mock
    private BookingItemRepository bookingItemRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock private MetricsService metricsService;


    @Mock private VirtualQueueService virtualQueueService;
   @Mock private SeatLockService seatLockService;
    @BeforeEach
    void setup() {
        seatLockService=new SeatLockService(
                redisTemplate,
                ticketTierRepository,
                seatInventoryRepository,
                simpMessagingTemplate,
                seatMapper,
                stringRedisTemplate,
                metricsService,
                virtualQueueService,
                bookingItemRepository
        );
    }

    @Test
    void lockGaTier_success(){
        UUID tierId=UUID.randomUUID();
        User user=new User();
        user.setId(UUID.randomUUID());

        TicketTier tier=TicketTier.builder()
                .availableQuantity(100)
                .totalQuantity(100)
                .maxPerUser(5)
                .build();

        when(ticketTierRepository.findById(tierId)).thenReturn(Optional.of(tier));

        when(virtualQueueService.isQueueActive(tierId)).thenReturn(false);

        doAnswer(invocation -> {
            return 1L;
        }).when(redisTemplate).execute(any(), anyList(), any(), any(), any(), any());
        SetOperations<String,String> setOperations=mock(SetOperations.class);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        assertDoesNotThrow(()->seatLockService.lockGaTier(tierId,1,user));

        //verify
        verify(ticketTierRepository).findById(tierId);

        verify(redisTemplate).execute(any(),anyList(),anyInt(),anyInt(),anyLong(),anyString());
        verify(setOperations).add("active_ga_tiers",tierId.toString());
    }

    @Test
    void lockGaTier_notEnoughInventory(){
        UUID tierId=UUID.randomUUID();
        User user=new User();
        user.setId(UUID.randomUUID());

        TicketTier tier = TicketTier.builder()
                .availableQuantity(10)
                .totalQuantity(10)
                .maxPerUser(5)
                .build();

        when(ticketTierRepository.findById(tierId)).thenReturn(Optional.of(tier));

        when(virtualQueueService.isQueueActive(tierId)).thenReturn(false);

        doAnswer(invocation -> {
            return -1L;
        }).when(redisTemplate).execute(any(), anyList(), any(), any(), any(), any());

        SeatUnavailableException exception=assertThrows(SeatUnavailableException.class,() -> seatLockService.lockGaTier(
                                tierId, 1, user));
        assertEquals("Not enough tickets available",exception.getMessage());


    }
    //@Disabled
    @Test
    void lockAssignedSeat_success() {
        UUID seatId=UUID.randomUUID();
        User user=new User();
        user.setId(UUID.randomUUID());

        TicketTier tier=TicketTier.builder().id(UUID.randomUUID())
                .availableQuantity(100)
                .totalQuantity(100)
                .maxPerUser(5)
                .build();

        SeatInventory seat=new SeatInventory();
        seat.setId(seatId);
        seat.setStatus(SeatStatus.AVAILABLE);
        Concert concert=new Concert();
        concert.setId(UUID.randomUUID());
        tier.setConcert(concert);
        seat.setTier(tier);

        ValueOperations<String, Object> valueOperations=mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.setIfAbsent(anyString(),anyString(),anyLong(),any())).thenReturn(true);

        when(seatInventoryRepository.findById(seatId)).thenReturn(Optional.of(seat));

        when(virtualQueueService.isQueueActive(tier.getId())).thenReturn(false);

        when(seatInventoryRepository.save(any(SeatInventory.class))).thenReturn(seat);


        TransactionSynchronizationManager.initSynchronization();

        try{assertDoesNotThrow(()->seatLockService.lockAssignedSeat(seatId,user));
        }finally{TransactionSynchronizationManager.clearSynchronization();}

        verify(seatInventoryRepository).findById(seatId);

        verify(seatInventoryRepository).save(any(SeatInventory.class));

        assertEquals(SeatStatus.LOCKED, seat.getStatus());

        assertEquals(user,seat.getLockedByUser());

        assertNotNull(seat.getLockedUntil()
        );

    }

    @Test
    void lockAssignedSeat_alreadyLocked() {

        UUID seatId=UUID.randomUUID();
        User user=new User();
        user.setId(UUID.randomUUID());

        @SuppressWarnings("unchecked")
        ValueOperations<String,Object>valueOps=(ValueOperations<String, Object>) mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)
        )).thenReturn(false);

        SeatUnavailableException exception=assertThrows(
                        SeatUnavailableException.class,
                        () -> seatLockService.lockAssignedSeat(
                                seatId,
                                user
                        )
                );
        assertEquals("Seat is being processed, please try again later",exception.getMessage()
        );
    }
    @Test
    void lockGaTier_quantityExceedsLimit() {
        UUID tierId=UUID.randomUUID();
        User user=new User();
        user.setId(UUID.randomUUID());
        TicketTier tier= TicketTier.builder().availableQuantity(100).totalQuantity(100)
                .maxPerUser(5)
                .build();
        when(ticketTierRepository.findById(tierId))
                .thenReturn(Optional.of(tier));
        when(virtualQueueService.isQueueActive(tierId))
                .thenReturn(false);

        SeatUnavailableException exception =
                assertThrows(SeatUnavailableException.class,()->seatLockService.lockGaTier(tierId, 10, user));

        assertEquals("Maximum 5 tickets allowed per user",exception.getMessage());


    }

}
