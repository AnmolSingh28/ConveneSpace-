package com.concertbooking.concert_booking.queue.service;

import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.metrics.MetricsService;
import com.concertbooking.concert_booking.queue.dto.QueueJoinResponse;
import com.concertbooking.concert_booking.queue.dto.QueuePositionResponse;
import com.concertbooking.concert_booking.seat.service.SeatLockService;
import com.concertbooking.concert_booking.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.concertbooking.concert_booking.queue.constants.QueueConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirtualQueueService {
    private final TicketTierRepository ticketTierRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final MetricsService metricsService;
    private String getQueueKey(UUID tierId) {
        return QUEUE_PREFIX + tierId;
    }
    //private final SeatLockService seatLockService;
    private static final String ADMISSION_ZSET_PREFIX = "queue:admissions:";

    public QueueJoinResponse joinQueue(UUID tierId, User user){
        TicketTier tier = ticketTierRepository.findById(tierId).orElseThrow(() -> new ResourceNotFoundException("Tier not found: " + tierId));
        String queueKey = getQueueKey(tierId);

        Long existingRank = redisTemplate.opsForZSet().rank(queueKey, user.getId().toString());

        if (existingRank != null){
            long total = redisTemplate.opsForZSet().size(queueKey);
            long estimatedWait = Math.max(existingRank.longValue() + 1, 1);

            return new QueueJoinResponse(tierId,existingRank.intValue()+1,existingRank.longValue(),total,estimatedWait ,"Already in queue");
        }

        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(queueKey, user.getId().toString(), score);
        metricsService.incrementQueueEntries();
        metricsService.incrementActiveQueueUsers();
        stringRedisTemplate.opsForSet().add("active_ga_tiers", tierId.toString());
        Long rank = redisTemplate.opsForZSet().rank(queueKey, user.getId().toString());
        Long total = redisTemplate.opsForZSet().size(queueKey);
        long estimatedWait = Math.max(rank.longValue() + 1, 1);
        return new QueueJoinResponse(tierId, rank.intValue() + 1, rank, total,estimatedWait ,"Joined success");
    }

    public QueuePositionResponse getPosition(UUID tierId, User user){
        String queueKey = getQueueKey(tierId);
        Long rank = redisTemplate.opsForZSet().rank(queueKey, user.getId().toString());
        if (rank == null){
            throw new ResourceNotFoundException("User is not in queue");
        }
        Long total = redisTemplate.opsForZSet().size(queueKey);
        long estimatedWait = (rank + 1) * ADMISSION_WINDOW_MINUTES;
        return new QueuePositionResponse(tierId, rank.intValue() + 1, rank, total, estimatedWait);
    }

    public void leaveQueue(UUID tierId, User user){
        String queueKey = getQueueKey(tierId);
        Long removed = redisTemplate.opsForZSet().remove(queueKey, user.getId().toString());
        if (removed == null || removed == 0) {
            throw new ResourceNotFoundException(
                    "User is not in queue"
            );
        }
        metricsService.decrementActiveQueueUsers();
    }

    public long getQueueSize(UUID tierId){
        String queueKey = getQueueKey(tierId);
        Long size = redisTemplate.opsForZSet().size(queueKey);
        return size == null ? 0 : size;
    }

    private String getTokenKey(UUID tierId, UUID userId){
        return QUEUE_TOKEN_PREFIX + tierId + ":" + userId;
    }

    public boolean hasValidQueueToken(UUID tierId, User user
    ) {
        String tokenKey = getTokenKey(tierId, user.getId());

        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
    }

    public void grantAdmission(UUID tierId, User user){
        String tokenKey = getTokenKey(tierId, user.getId());

        redisTemplate.opsForValue().set(tokenKey, "ALLOWED", ADMISSION_WINDOW_MINUTES, TimeUnit.MINUTES);
    }

    public void consumeToken(UUID tierId, User user) {
        redisTemplate.delete(getTokenKey(tierId, user.getId()));
    }

    public void admitNextUsers(UUID tierId, int count){
        String queueKey = getQueueKey(tierId);
        Set<Object> users = redisTemplate.opsForZSet().range(queueKey, 0, count - 1);
        if (users == null || users.isEmpty()) return;

        for (Object userIdObj : users) {

            UUID userId=UUID.fromString(userIdObj.toString());
            String tokenKey=getTokenKey(tierId, userId);
            redisTemplate.opsForValue().set(tokenKey, "ALLOWED", ADMISSION_WINDOW_MINUTES, TimeUnit.MINUTES);
            long expiryTimestamp=System.currentTimeMillis()+TimeUnit.MINUTES.toMillis(ADMISSION_WINDOW_MINUTES);
            redisTemplate.opsForZSet().add(
                    "queue:admissions:" + tierId, userId.toString(), expiryTimestamp
            );
            redisTemplate.opsForZSet().remove(queueKey, userId.toString());

            log.info("Admitted user {} for tier {}", userId, tierId);
            simpMessagingTemplate.convertAndSend("/topic/queue." + tierId + ".admission",
                    Map.of(
                            "type", "QUEUE_ADMISSION",
                            "tierId", tierId,
                            "admittedUserId", userId.toString(),
                            "message", "You may now book tickets",
                            "timestamp", System.currentTimeMillis()
                    )
            );
        }
    }

    public int getRealAvailableQuantity(UUID tierId){
        TicketTier tier = ticketTierRepository.findById(tierId).orElseThrow(() -> new ResourceNotFoundException("Not found"));
        String reservedKey = "tier:reserved:" + tierId;

        Object reservedObj = redisTemplate.opsForValue().get(reservedKey);

        int reserved = reservedObj == null ? 0 : Integer.parseInt(reservedObj.toString());

        return Math.max(0, tier.getAvailableQuantity() - reserved);
    }

    public void broadcastQueueStats(UUID tierId){
        simpMessagingTemplate.convertAndSend(
                "/topic/queue." + tierId,
                Map.of(
                        "queueSize",
                        getQueueSize(tierId),
                        "availableTickets",
                        getRealAvailableQuantity(tierId),
                        "timestamp",
                        System.currentTimeMillis()
                )
        );
    }

    public void broadcastPositionUpdates(UUID tierId){
        String queueKey = getQueueKey(tierId);
        Set<Object> users = redisTemplate.opsForZSet().range(queueKey, 0, 99);
        if (users == null || users.isEmpty()) return;

        int position = 1;
        int totalQueueSize = users.size();
        for (Object userObj : users) {
            String userId = userObj.toString();

            long estimatedWait = position * ADMISSION_WINDOW_MINUTES;
            simpMessagingTemplate.convertAndSend(
                    "/topic/queue." + tierId + ".position." + userId,
                    Map.of(
                            "type", "POSITION_UPDATE",
                            "position", position,
                            "peopleAhead", position - 1,
                            "queueSize", totalQueueSize,
                            "estimatedWaitMinutes", estimatedWait,
                            "tierId", tierId,
                            "timestamp", System.currentTimeMillis()
                    )
            );
            position++;
        }
    }


  public boolean isQueueActive(UUID tierId){

      Object reserved = redisTemplate.opsForValue().get("tier:reserved:" + tierId);
      int reservedCount = reserved != null ? Integer.parseInt(reserved.toString()) : 0;

      TicketTier tier = ticketTierRepository.findById(tierId).orElseThrow(() -> new ResourceNotFoundException("Tier not found"));
      int effectiveAvailable = tier.getAvailableQuantity() - reservedCount;
      long viewers = getViewerCount(tier.getConcert().getId());
      double ratio = (double) viewers / Math.max(effectiveAvailable, 10);
      return effectiveAvailable <= 0 || ratio >= 5;
  }

    public long countActiveAdmissions(UUID tierId){
        String key = ADMISSION_ZSET_PREFIX + tierId;
        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0;

    }
    public void removeExpiredAdmissions(UUID tierId){
        String key = ADMISSION_ZSET_PREFIX + tierId;
        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);
    }
    public void removeFromActiveAdmissions(UUID tierId, UUID userId){
        String key = ADMISSION_ZSET_PREFIX + tierId;
        Long removedCount=redisTemplate.opsForZSet().remove(key, userId.toString());
        log.info("REMOVE ADMISSION -> user={}, removedCount={}", userId, removedCount);
    }
    private String getActiveUsersKey(UUID tierId) {
        return "active_users:" + tierId;
    }

    public void trackActiveUser(UUID tierId, UUID userId) {
        redisTemplate.opsForSet().add(getActiveUsersKey(tierId), userId.toString());

        redisTemplate.expire(getActiveUsersKey(tierId), 5, TimeUnit.MINUTES);
    }

    public void removeActiveUser(UUID tierId, UUID userId) {
        redisTemplate.opsForSet().remove(getActiveUsersKey(tierId), userId.toString());
    }

    public long getActiveUsers(UUID tierId){
        Long count = redisTemplate.opsForSet().size(getActiveUsersKey(tierId));

        return count == null ? 0 : count;
    }

    public double getDemandRatio(UUID tierId){

        TicketTier tier = ticketTierRepository.findById(tierId).orElseThrow(()->new ResourceNotFoundException("Tier not found"));

        UUID concertId = tier.getConcert().getId();
        long viewers = getViewerCount(tier.getConcert().getId());
        int availableTickets = Math.max(getRealAvailableQuantity(tierId), 10);
        return (double) viewers / availableTickets;
    }

    public int getDynamicAdmissionCount(UUID tierId){
        double ratio = getDemandRatio(tierId);

        if (ratio >= 50.0){
            return 50;
        }
        if (ratio >= 20.0){
            return 150;
        }
        if (ratio >= 10.0){
            return 300;
        }
        return MAX_ADMISSIONS_PER_BATCH;
    }
    public long getViewerCount(UUID concertId){
        String key = "viewing:" + concertId;
        Long count = redisTemplate.opsForSet().size(key);
        return count == null ? 0 : count;
    }

}
