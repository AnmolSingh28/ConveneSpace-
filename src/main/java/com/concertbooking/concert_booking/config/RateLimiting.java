package com.concertbooking.concert_booking.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Component
@Profile("!test")
@Slf4j
public class RateLimiting extends OncePerRequestFilter {
    private  final ProxyManager<String> proxyManager;
    public RateLimiting(LettuceConnectionFactory lettuceConnectionFactory,
                           @Value("${spring.data.redis.password}") String password,
                           @Value("${spring.data.redis.host}") String host,
                           @Value("${spring.data.redis.port}") int port){

        //Dedicated Redis connection used by bucket4j to store distributed ratelimit buckets
        RedisClient redisClient=RedisClient.create("redis://:"+password+"@"+host+":"+port);

        StatefulRedisConnection<String,byte[]> connection=redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );
        //Redis backed bucket storage so that limits are shared across all app instances
        this.proxyManager= LettuceBasedProxyManager
                .builderFor(connection)
                .build();

    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        //Skip rate limiting
        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/actuator") ||
                path.startsWith("/ws") ||
                path.startsWith("/api/v1/concerts")||
                path.startsWith("/api/v1/concerts/featured")||
                path.startsWith("/api/v1/concerts/search")
                ) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        String bucketKey = "ratelimit:" + getBucketKey(ip, path);

        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                        .addLimit(Bandwidth.classic(getLimit(path), Refill.greedy(getLimit(path), Duration.ofMinutes(1))))
                        .build();

        var bucket=proxyManager.builder().build(bucketKey, configSupplier);

        if(bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        }else{
            log.warn("Rate limit exceeded — IP: {} path: {}", ip, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please slow down.\",\"data\":null}");
        }
    }
    //Different endpoints get different request budgets
    private long getLimit(String path) {
        if (path.startsWith("/api/v1/auth")) return 10;
        if (path.startsWith("/api/v1/bookings")) return 20;
        if (path.startsWith("/api/v1/payments")) return 10;
        if (path.startsWith("/api/v1/seats")) return 30;
        return 100;
    }
    //Creates separate buckets for auth, booking, payment and general traffic
    private String getBucketKey(String ip, String path) {
        if (path.startsWith("/api/v1/auth")) return "auth:" + ip;
        if (path.startsWith("/api/v1/bookings")) return "booking:" + ip;
        if (path.startsWith("/api/v1/payments")) return "payment:" + ip;
        if (path.startsWith("/api/v1/seats")) return "seats:" + ip;
        return "general:" + ip;
    }
    //Handles proxy/cdn deployments by extracting the real client IP
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) return realIp;
        return request.getRemoteAddr();
    }
}
