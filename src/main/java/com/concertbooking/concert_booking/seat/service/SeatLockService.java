package com.concertbooking.concert_booking.seat.service;

import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import com.concertbooking.concert_booking.common.enums.SeatStatus;
import com.concertbooking.concert_booking.common.exception.SeatUnavailableException;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.metrics.MetricsService;
import com.concertbooking.concert_booking.queue.service.VirtualQueueService;
import com.concertbooking.concert_booking.seat.dto.SeatResponse;
import com.concertbooking.concert_booking.seat.entity.SeatInventory;
import com.concertbooking.concert_booking.seat.mapper.SeatMapper;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.user.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final TicketTierRepository ticketTierRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final SeatMapper seatMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MetricsService metricsService;
    private final VirtualQueueService virtualQueueService;
    private final BookingItemRepository bookingItemRepository;
    private static final String TIER_LOCK_PREFIX = "tier:lock:";
    private static final String SEAT_LOCK_PREFIX = "seat:lock:";
    private static final String VIEWING_KEY = "concert:viewing:";
    private static final String SEAT_PRESENCE_PREFIX = "seat:presence:";
    private static final int REDIS_LOCK_TTL_SECONDS = 5;

    private static final String GA_LOCK_SCRIPT =
            "local reservedKey = KEYS[1]\n" +
                    "local userKey = KEYS[2]\n" +
                    "local expiryZset = KEYS[3]\n" +
                    "local dbAvailable = tonumber(ARGV[1])\n" +
                    "local reqQty = tonumber(ARGV[2])\n" +
                    "local expiresAt = tonumber(ARGV[3])\n" +
                    "local userId = ARGV[4]\n" +
                    "local currentReserved = tonumber(redis.call('GET', reservedKey) or '0')\n" +
                    "if redis.call('EXISTS', userKey) == 1 then return -2 end\n" +
                    "if (currentReserved + reqQty) > dbAvailable then return -1 end\n" +
                    "redis.call('INCRBY', reservedKey, reqQty)\n" +
                    "redis.call('SET', userKey, reqQty)\n" +
                    "redis.call('ZADD', expiryZset, expiresAt, userId)\n" +
                    "return currentReserved + reqQty";

    // LUA SCRIPT:Atomic GA Release (Cleans up locks securely)
    private static final String GA_RELEASE_SCRIPT =
            "local reservedKey = KEYS[1]\n" +
                    "local userKey = KEYS[2]\n" +
                    "local expiryZset = KEYS[3]\n" +
                    "local userId = ARGV[1]\n" +
                    "local qty = tonumber(redis.call('GET', userKey) or '0')\n" +
                    "if qty > 0 then\n" +
                    "  redis.call('DECRBY', reservedKey, qty)\n" +
                    "  redis.call('DEL', userKey)\n" +
                    "  redis.call('ZREM', expiryZset, userId)\n" +
                    "  return qty\n" +
                    "end\n" +
                    "return 0";

    //FOR GA TICKETS
    @Transactional
    public void lockGaTier(UUID tierId, int quantity, User user) {
        TicketTier tier = ticketTierRepository.findById(tierId).orElseThrow(()->new SeatUnavailableException("Tier Not found"));
        if (virtualQueueService.isQueueActive(tierId)) {
            if (!virtualQueueService.hasValidQueueToken(tierId,user)){
                throw new SeatUnavailableException(
                        "Please wait in queue before booking"
                );
            }
            virtualQueueService.consumeToken(tierId,user);

        }
        if (quantity >tier.getMaxPerUser()) {
            throw new SeatUnavailableException("Maximum " + tier.getMaxPerUser() + " tickets allowed per user");
        }
        int ttlMinutes = calculateDynamicTtl(tier);
        long expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(ttlMinutes);

        List<String> keys = Arrays.asList(
                "tier:reserved:" + tierId,
                "tier:user:" + tierId + ":" + user.getId(),
                "tier:expirations:" + tierId
        );

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(GA_LOCK_SCRIPT, Long.class);
        Long newReservedCount = redisTemplate.execute(script, keys,
                tier.getAvailableQuantity(),
                quantity,
                expiresAt,
                user.getId().toString()
        );

        if (newReservedCount == -2L) {
            throw new SeatUnavailableException("You already have an active reservation for this tier");
        }
        if (newReservedCount == -1L) {
            throw new SeatUnavailableException("Not enough tickets available");
        }

        //Track active tiers so our `@Scheduled` expiration job knows where to look without using KEYS
        stringRedisTemplate.opsForSet().add("active_ga_tiers", tierId.toString());

        //Correct Broadcast Math: (DB Capacity minus Redis Holds)
        int currentAvailable = tier.getAvailableQuantity() - newReservedCount.intValue();
        metricsService.incrementReservations();
        log.info("GA lock acquired -> tier: {}, user: {}, qty: {}, ttl: {}min",
                tierId, user.getId(), quantity, ttlMinutes);


    }

    //Explicit GA Release
    @Transactional
    public void releaseGaLock(UUID tierId, User user) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(GA_RELEASE_SCRIPT, Long.class);
        List<String> keys = Arrays.asList(
                "tier:reserved:" + tierId,
                "tier:user:" + tierId + ":" + user.getId(),
                "tier:expirations:" + tierId
        );
        Long releasedQty = redisTemplate.execute(script, keys, user.getId().toString());

        if (releasedQty != null && releasedQty > 0) {
            virtualQueueService.removeFromActiveAdmissions(
                    tierId,
                    user.getId()
            );
            log.info("GA lock released explicitly -> tier: {}, user: {}, qty: {}", tierId, user.getId(), releasedQty);
            TicketTier tier = ticketTierRepository.findById(tierId).orElseThrow();

            //Recalculate true available quantity
            int currentReserved = getReservedCount(tierId.toString());
            broadcastAvailability(tier.getConcert().getId(), tierId, tier.getAvailableQuantity() - currentReserved);

        }
    }

    //Called Exclusively by PaymentService on Success!
    @Transactional
    public void consumeGaLockOnPaymentSuccess(UUID tierId, User user) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(GA_RELEASE_SCRIPT, Long.class);
        List<String> keys = Arrays.asList(
                "tier:reserved:" + tierId,
                "tier:user:" + tierId + ":" + user.getId(),
                "tier:expirations:" + tierId
        );
        Long releasedQty = redisTemplate.execute(script, keys, user.getId().toString());

        if (releasedQty != null && releasedQty > 0) {
            virtualQueueService.removeFromActiveAdmissions(
                    tierId,
                    user.getId()
            );
            log.info("GA lock consumed permanently (Paid) -> tier: {}, user: {}", tierId, user.getId());

        }
    }

    @Transactional(rollbackFor = SeatUnavailableException.class)
    public void lockAssignedSeat(UUID seatInventoryId, User user) {
        String redisLockKey=SEAT_LOCK_PREFIX + seatInventoryId;

        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(redisLockKey, user.getId().toString(), REDIS_LOCK_TTL_SECONDS, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(locked)) {
            throw new SeatUnavailableException("Seat is being processed, please try again later");
        }

        try {
            SeatInventory seat = seatInventoryRepository.findById(seatInventoryId)
                    .orElseThrow(() -> new SeatUnavailableException("Seat not found"));
            UUID tierId = seat.getTier().getId();
            if (virtualQueueService.isQueueActive(tierId)) {
                if (!virtualQueueService.hasValidQueueToken(tierId,user)) {
                    throw new SeatUnavailableException("Please wait in queue before booking");
                }
                virtualQueueService.consumeToken(tierId,user);
            }
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatUnavailableException("Seat " + seat.getRowLabel() + seat.getSeatNumber() + " is not available");
            }
            int alreadyLocked=seatInventoryRepository
                    .countByLockedByUserIdAndConcertId(user.getId(),seat.getTier().getConcert().getId());

            int alreadyConfirmed=bookingItemRepository
                    .countConfirmedSeatsForUserAndConcert(user.getId(),seat.getTier().getConcert().getId());

            if (alreadyLocked + alreadyConfirmed >= seat.getTier().getMaxPerUser()) {
                throw new SeatUnavailableException("Max per user seat limit reached");
            }
            int ttlMinutes=calculateDynamicTtl(seat.getTier());

            seat.setStatus(SeatStatus.LOCKED);
            seat.setLockedByUser(user);
            String presenceKey = SEAT_PRESENCE_PREFIX + seatInventoryId + ":" + user.getId();

            redisTemplate.opsForValue().set(presenceKey,"alive",1,TimeUnit.MINUTES);
            seat.setLockedUntil(LocalDateTime.now().plusMinutes(ttlMinutes));

            try {
                seatInventoryRepository.save(seat);
                metricsService.incrementReservations();
            } catch (ObjectOptimisticLockingFailureException e) {
                throw new SeatUnavailableException("Seat was just taken, please select another");
            }

            log.info("Seat locked -> seat: {}, user: {}, ttl: {}min", seatInventoryId, user.getId(), ttlMinutes);

            UUID concertId = seat.getTier().getConcert().getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        redisTemplate.delete(redisLockKey);
                        broadcastSeatStatus(concertId, seatInventoryId, SeatStatus.LOCKED);
                    } catch (Exception e) {
                        log.error("afterCommit failed for seat: {}", seatInventoryId, e);
                        redisTemplate.delete(redisLockKey);
                    }
                }
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        redisTemplate.delete(redisLockKey);
                    }
                }
            });
        } catch (SeatUnavailableException e) {
            redisTemplate.delete(redisLockKey);
            throw e;
        }
    }

    @Transactional
    public void releaseAssignedSeat(UUID seatInventoryId) {
        SeatInventory seat = seatInventoryRepository.findById(seatInventoryId).orElseThrow();
        seat.setStatus(SeatStatus.AVAILABLE);
        if(seat.getLockedByUser() != null){
            String presenceKey = SEAT_PRESENCE_PREFIX + seat.getId() + ":" + seat.getLockedByUser().getId();
            redisTemplate.delete(presenceKey);
        }
        seat.setLockedByUser(null);
        seat.setLockedUntil(null);
        seatInventoryRepository.save(seat);

        broadcastSeatStatus(seat.getTier().getConcert().getId(), seatInventoryId, SeatStatus.AVAILABLE);
        log.info("Seat released: {}", seatInventoryId);


    }


    @Scheduled(fixedRate = 10000) // Run every 10 seconds for snappy waitlist updates
    @Transactional
    public void releaseExpiredAssignedSeats() {
        List<SeatInventory> expired = seatInventoryRepository.findExpiredLocks(LocalDateTime.now());
        if (expired.isEmpty()) return;

        for (SeatInventory seat : expired) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setLockedByUser(null);
            seat.setLockedUntil(null);
            seatInventoryRepository.save(seat);

            broadcastSeatStatus(seat.getTier().getConcert().getId(), seat.getId(), SeatStatus.AVAILABLE);

        }
        log.info("Released {} expired assigned seat locks", expired.size());
    }

    @Scheduled(fixedRate = 10000)
    public void releaseExpiredGaLocks() {
        Set<String> activeTiers = stringRedisTemplate.opsForSet().members("active_ga_tiers");
        if (activeTiers == null || activeTiers.isEmpty()) return;

        long now = System.currentTimeMillis();
        DefaultRedisScript<Long> releaseScript = new DefaultRedisScript<>(GA_RELEASE_SCRIPT, Long.class);

        for (Object tierObj : activeTiers) {
            String tierIdStr = tierObj.toString();
            String zsetKey = "tier:expirations:" + tierIdStr;


            Set<Object> expiredUsers = redisTemplate.opsForZSet().rangeByScore(zsetKey, 0, now);
            if (expiredUsers != null && !expiredUsers.isEmpty()) {
                UUID tierId = UUID.fromString(tierIdStr);
                Optional<TicketTier> tierOpt = ticketTierRepository.findById(tierId);

                for (Object userObj : expiredUsers) {
                    String userId = userObj.toString();
                    List<String> keys = Arrays.asList(
                            "tier:reserved:" + tierIdStr,
                            "tier:user:" + tierIdStr + ":" + userId,
                            zsetKey
                    );

                    Long releasedQty = redisTemplate.execute(releaseScript, keys, userId);

                    if (releasedQty != null && releasedQty > 0) {
                        log.info("Auto-released expired GA lock for tier: {}, user: {}, qty: {}", tierIdStr, userId, releasedQty);

                        if (tierOpt.isPresent()) {
                            int currentReserved = getReservedCount(tierIdStr);
                            broadcastAvailability(tierOpt.get().getConcert().getId(), tierId, tierOpt.get().getAvailableQuantity() - currentReserved);
                        }

                    }
                }
            } else {
                //If there are zero expirations in ZSET, we can technically remove it from active tiers tracking to keep Redis tracking clean.
                Long count = redisTemplate.opsForZSet().zCard(zsetKey);
                long queueSize = virtualQueueService.getQueueSize(UUID.fromString(tierIdStr));
                if ((count == null || count == 0) && queueSize == 0) {
                    redisTemplate.opsForSet().remove("active_ga_tiers", tierIdStr);
                }
            }
        }
    }


    private int calculateDynamicTtl(TicketTier tier) {
        if (tier.getTotalQuantity() == 0) return 10;
        double availablePercent = (double) tier.getAvailableQuantity() / tier.getTotalQuantity() * 100;
        if (availablePercent > 50) return 10;
        if (availablePercent > 20) return 7;
        if (availablePercent > 5) return 5;
        return tier.getLockTtlMinutes() > 0 ? Math.min(tier.getLockTtlMinutes(), 2) : 2;
    }

    private int getReservedCount(String tierId) {
        Object val = redisTemplate.opsForValue().get("tier:reserved:" + tierId);
        return val != null ? Integer.parseInt(val.toString()) : 0;
    }

    private void broadcastAvailability(UUID concertId, UUID tierId, int available) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tierId", tierId);
        payload.put("availableQuantity", Math.max(0, available));
        payload.put("timestamp", System.currentTimeMillis());
        simpMessagingTemplate.convertAndSend("/topic/concert." + concertId + ".availability", payload);
    }

    private void broadcastSeatStatus(UUID concertId, UUID seatId, SeatStatus status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("seatId", seatId);
        payload.put("status", status.name());
        payload.put("timestamp", System.currentTimeMillis());
        simpMessagingTemplate.convertAndSend("/topic/concert." + concertId + ".seats", payload);
    }

    public List<SeatResponse> getSeatsByTier(UUID tierId) {
        return seatMapper.toResponseList(seatInventoryRepository.findByTierId(tierId));
    }

    public List<SeatResponse> getAvailableSeats(UUID tierId) {
        return seatMapper.toResponseList(seatInventoryRepository.findByTierIdAndStatus(tierId, SeatStatus.AVAILABLE));
    }

    public void trackViewing(UUID concertId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return;
        String key = VIEWING_KEY + concertId;
        redisTemplate.opsForSet().add(key, sessionId);
        redisTemplate.expire(key, 45, TimeUnit.SECONDS);
        Long count = redisTemplate.opsForSet().size(key);
        simpMessagingTemplate.convertAndSend(
                "/topic/concert." + concertId + ".viewers",
                Map.of(
                        "concertId", concertId.toString(),
                        "viewerCount", count != null ? count : 0,
                        "timestamp", System.currentTimeMillis()
                )
        );

    }
    public void updateSeatPresence(UUID seatId,User user){
        String key=SEAT_PRESENCE_PREFIX+seatId +":" +user.getId();

        redisTemplate.opsForValue().set(key,"alive",1,TimeUnit.MINUTES);
    }
    @Scheduled(fixedRate = 15000)
    @Transactional
    public void releaseAbandonedSeats() {
        List<SeatInventory> lockedSeats = seatInventoryRepository.findByStatus(SeatStatus.LOCKED);

        for (SeatInventory seat : lockedSeats) {
            if (seat.getLockedByUser() == null) continue;


            if (seat.getLockedUntil() != null && LocalDateTime.now().isAfter(seat.getLockedUntil())) {
                continue;
            }

            String presenceKey = SEAT_PRESENCE_PREFIX + seat.getId() + ":" + seat.getLockedByUser().getId();
            Boolean exists = redisTemplate.hasKey(presenceKey);

            if (Boolean.FALSE.equals(exists)) {
                log.info("User disappeared, releasing seat {}", seat.getId());
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setLockedByUser(null);
                seat.setLockedUntil(null);
                seatInventoryRepository.save(seat);
                broadcastSeatStatus(seat.getTier().getConcert().getId(), seat.getId(), SeatStatus.AVAILABLE);
            }
        }
    }
    public long getViewerCount(UUID concertId) {
        String key = "concert:viewing:" + concertId; // wahi key jo SeatLockService use kar raha hai

        Long count = redisTemplate.opsForSet().size(key);

        return count == null ? 0 : count;
    }
    @Transactional
    public void releaseAllAssignedSeatsForUser(User user) {

        List<SeatInventory> lockedSeats =
                seatInventoryRepository.findByLockedByUserId(user.getId());

        for (SeatInventory seat : lockedSeats) {
            releaseAssignedSeat(seat.getId());
        }

        log.info("Released {} assigned seats for user {}",
                lockedSeats.size(),
                user.getId());
    }
}


