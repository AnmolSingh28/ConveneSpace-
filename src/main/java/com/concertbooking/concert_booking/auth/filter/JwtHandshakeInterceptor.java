package com.concertbooking.concert_booking.auth.filter;

import com.concertbooking.concert_booking.auth.service.CustomUserDetailsService;
import com.concertbooking.concert_booking.auth.util.JwtUtil;
import com.concertbooking.concert_booking.user.entity.User;
import com.nimbusds.jwt.JWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtUtil jwtUtil;
// Verify JWT during WebSocket handshake and store user info in WebSocket session
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest
                    .getServletRequest()
                    .getParameter("token");

            if (token!=null) {
                try {
                    if (!jwtUtil.isTokenExpiredOrInvalid(token)) {

                        UUID userId = jwtUtil.extractUserId(token);
                        String email =jwtUtil.extractEmail(token);
                        attributes.put("userId", userId);
                        attributes.put("email", email);
                        log.debug("WebSocket authenticated: {}", email);
                        return true;
                    }
                } catch (Exception e) {
                    log.error("WebSocket auth failed: {}", e.getMessage());
                }
            }

            attributes.put("anonymous", true);
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler wsHandler,
            Exception exception) {
    }
    }

