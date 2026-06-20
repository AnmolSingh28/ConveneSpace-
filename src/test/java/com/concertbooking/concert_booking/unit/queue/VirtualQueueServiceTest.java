package com.concertbooking.concert_booking.unit.queue;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.metrics.MetricsService;
import com.concertbooking.concert_booking.queue.dto.QueueJoinResponse;
import com.concertbooking.concert_booking.queue.service.VirtualQueueService;
import com.concertbooking.concert_booking.seat.service.SeatLockService;
import com.concertbooking.concert_booking.user.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@ExtendWith(MockitoExtension.class)
public class VirtualQueueServiceTest {
    @Mock private TicketTierRepository ticketTierRepository;

    @Mock private RedisTemplate<String,Object> redisTemplate;

    @Mock private SimpMessagingTemplate simpMessagingTemplate;

    @Mock private StringRedisTemplate stringRedisTemplate;

    @Mock private VirtualQueueService virtualQueueService;

    @Mock private MetricsService metricsService;

  
    @BeforeEach
    void setup() {

        virtualQueueService = new VirtualQueueService(
                ticketTierRepository,
                redisTemplate,
                simpMessagingTemplate,
                stringRedisTemplate,
                metricsService

        );
    }
    @Test
    void joinQueue_success() {

        UUID tierId = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());
        TicketTier tier = TicketTier.builder().id(tierId).build();

        @SuppressWarnings("unchecked")
        ZSetOperations<String,Object> zSetOps=(ZSetOperations<String,Object>) mock(ZSetOperations.class);

        @SuppressWarnings("unchecked")
        SetOperations<String,String> setOps =(SetOperations<String,String>) mock(SetOperations.class);

        when(ticketTierRepository.findById(tierId)).thenReturn(Optional.of(tier));

        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        when(stringRedisTemplate.opsForSet()).thenReturn(setOps);

        when(zSetOps.rank(anyString(), anyString())).thenReturn(null).thenReturn(0L);

        when(zSetOps.size(anyString())).thenReturn(1L);

        QueueJoinResponse response=virtualQueueService.joinQueue(tierId,user);

        assertNotNull(response);

        verify(zSetOps).add(anyString(), eq(user.getId().toString()), anyDouble()
        );
    }
    @Test
    void consumeToken_success() {

        UUID tierId = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());

        assertDoesNotThrow(()->virtualQueueService.consumeToken(tierId, user));

        verify(redisTemplate).delete(anyString());
    }
    @Test
    void leaveQueue_success() {

        UUID tierId = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());

        @SuppressWarnings("unchecked")
        ZSetOperations<String,Object> zSetOps = (ZSetOperations<String,Object>) mock(ZSetOperations.class);

        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        when(zSetOps.remove(anyString(), eq(user.getId().toString())
        )).thenReturn(1L);

        assertDoesNotThrow(() -> virtualQueueService.leaveQueue(tierId, user)
        );

        verify(zSetOps).remove(anyString(), eq(user.getId().toString()));
    }
}
