package com.concertbooking.concert_booking.concert.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConcertPresenceTracker {
    private final RedisTemplate<String,Object> redisTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private static final String ACTIVE_USERS_KEY="concert:active:";

    @EventListener
    public void handleConnect(SessionConnectEvent event){
        StompHeaderAccessor accessor=StompHeaderAccessor.wrap(event.getMessage());
        String concertId= accessor.getFirstNativeHeader("concertId");
        String userId= accessor.getFirstNativeHeader("userId");

        if(concertId!=null && userId!=null){
            redisTemplate.opsForSet().add(ACTIVE_USERS_KEY,concertId,userId);
            log.debug("User {} connected to concert {}",userId,concertId);
        }
    }
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event){
        StompHeaderAccessor accessor=StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object>sessionAttributes=accessor.getSessionAttributes();
        if (sessionAttributes!=null) {
            String concertId=(String) sessionAttributes.get("concertId");
            String userId=(String) sessionAttributes.get("userId");
            if (concertId !=null&&userId!=null) {
                redisTemplate.opsForSet().remove(ACTIVE_USERS_KEY +concertId,userId);
                log.debug("User {} disconnected from concert {}",userId,concertId);
            }
        }
    }
    @Scheduled(fixedRate = 10000)
    public void broadcastActiveUsers(){
        Set<String> keys=redisTemplate.keys(ACTIVE_USERS_KEY+ "*");
        if(keys==null ||keys.isEmpty()) return;

        for(Object key:keys){
            String keyStr=key.toString();
            String concertId=keyStr.replace(ACTIVE_USERS_KEY,"");
            Long count=redisTemplate.opsForSet().size(keyStr);
            if (count != null) {
                simpMessagingTemplate.convertAndSend(
                        "/topic/concert." + concertId + ".activeUsers",
                        Map.of(
                                "concertId", concertId,
                                "activeUsers", count,
                                "timestamp", System.currentTimeMillis()
                        )
                );
            }
        }
    }
}
