package com.concertbooking.concert_booking.queue.scheduler;

import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.queue.service.VirtualQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.concertbooking.concert_booking.queue.constants.QueueConstants.MAX_ADMISSIONS_PER_BATCH;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueScheduler {
    private final VirtualQueueService virtualQueueService;
    private final StringRedisTemplate stringRedisTemplate;
    @Scheduled(fixedRate=5000)
    public void processQueue(){
        log.info("Scheduler running...");
        Set<String> activeTierIds = stringRedisTemplate.opsForSet().members("active_ga_tiers");
        log.info("Active tiers: {}", activeTierIds);
        if (activeTierIds == null || activeTierIds.isEmpty()) return;
        for ( String tierObj : activeTierIds){
            UUID tierId = UUID.fromString(tierObj.replace("\"", ""));
            long queueSize=virtualQueueService.getQueueSize(tierId);
            if(queueSize == 0){
                continue;
            }
            virtualQueueService.removeExpiredAdmissions(tierId);
            int availableTickets = virtualQueueService.getRealAvailableQuantity(tierId);
            long activeAdmissions=virtualQueueService.countActiveAdmissions(tierId);
            int effectiveAvailable=(int) Math.max(0,availableTickets-activeAdmissions);

            log.info("Tier: {}, queueSize: {}, available: {}", tierId, queueSize, availableTickets);
            if (effectiveAvailable <= 0){
                virtualQueueService.broadcastQueueStats(tierId);
                virtualQueueService.broadcastPositionUpdates(tierId);
                continue;
            }
            int dynamicBatchSize = virtualQueueService.getDynamicAdmissionCount(tierId);

            int admitCount = Math.min(effectiveAvailable, dynamicBatchSize);
            log.info(
                    "DemandRatio={} ActiveUsers={} AvailableTickets={}",
                    virtualQueueService.getDemandRatio(tierId),
                    virtualQueueService.getActiveUsers(tierId),
                    availableTickets
            );
            virtualQueueService.admitNextUsers(tierId,admitCount);
            virtualQueueService.broadcastQueueStats(tierId);
            virtualQueueService.broadcastPositionUpdates(tierId);
            log.info("Queue processed -> tier={}, queueSize={}, availableTickets={}, admitted={}",
                    tierId,
                    queueSize,
                    availableTickets,
                    admitCount
            );
        }
    }

}
