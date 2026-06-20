package com.concertbooking.concert_booking.auth.filter;


import com.concertbooking.concert_booking.auth.service.CustomUserDetailsService;
import com.concertbooking.concert_booking.auth.util.JwtUtil;
import com.concertbooking.concert_booking.common.enums.UserRole;
import com.concertbooking.concert_booking.user.entity.User;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j

public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
            ) throws ServletException , IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String jwt = authHeader.substring(7);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey("blacklist:token:" + jwt))) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            if (jwtUtil.isTokenExpiredOrInvalid(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }
            if (!"ACCESS".equals(jwtUtil.extractTokenType(jwt))) {
                filterChain.doFilter(request, response);
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String email = jwtUtil.extractEmail(jwt);
                UUID userId = jwtUtil.extractUserId(jwt);
                String role = jwtUtil.extractRole(jwt);

                User principal = new User();
                principal.setId(userId);
                principal.setEmail(email);
                principal.setRole(UserRole.valueOf(role));

                UsernamePasswordAuthenticationToken authToken=new UsernamePasswordAuthenticationToken(principal,null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user: {} for path: {}", email, request.getRequestURI());
            }
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());

        }
        filterChain.doFilter(request, response);
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/actuator");

    }

}
