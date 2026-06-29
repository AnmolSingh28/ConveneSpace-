package com.concertbooking.concert_booking.auth.service;

import com.concertbooking.concert_booking.auth.dto.*;
import com.concertbooking.concert_booking.auth.util.JwtUtil;
import com.concertbooking.concert_booking.common.enums.UserRole;
import com.concertbooking.concert_booking.common.exception.AuthException;
import com.concertbooking.concert_booking.common.exception.BookingException;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.seat.service.SeatLockService;
import com.concertbooking.concert_booking.user.entity.User;
import com.concertbooking.concert_booking.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SeatLockService seatLockService;
    @Transactional
    public void initiateRegistration(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new AuthException("Phone number already registered");
        }
        if(request.getRole()==UserRole.ADMIN){
            throw new AuthException("Invalid role");
        }
        //Save user as unverified
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .emailVerified(false)
                .active(true)
                .build();

        userRepository.save(user);

        //Generate OTP,store in Redis and after that send the email
        String otp = otpService.generateAndStoreOtp(request.getEmail());
        emailService.sendOtpEmail(request.getEmail(), request.getName(), otp);

        log.info("Registration initiated for: {}", request.getEmail());
    }

    //Verify OTP, activate account
    @Transactional
    public AuthResponse verifyEmailAndActivate(OtpVerifyRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getOtp());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new AuthException("Email already verified");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified for: {}", request.getEmail());
        return buildAuthResponse(user);
    }

    //Login
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw new AuthException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            throw new AuthException("Please verify your email before logging in");
        }

        log.info("User logged in: {}", request.getEmail());
        return buildAuthResponse(user);
    }

    //Refresh access token
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        String email;
        try {
            email = jwtUtil.extractEmail(refreshToken);
        } catch (Exception e) {
            throw new AuthException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!jwtUtil.isRefreshTokenValid(refreshToken, user)) {
            throw new AuthException("Refresh token expired or invalid");
        }

        //Issue new access token only while the refresh token stays same
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user))
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getJwtExpiration())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    //Resend OTP
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {throw new AuthException("Email already verified");}

        String otp = otpService.generateAndStoreOtp(email);
        emailService.sendOtpEmail(email, user.getName(), otp);
        log.info("OTP resent to: {}", email);
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user))
                .refreshToken(jwtUtil.generateRefreshToken(user))
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getJwtExpiration())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
   //Logout
    public void logout(String accessToken,User user) {
        if(user!= null) {
            seatLockService.releaseAllAssignedSeatsForUser(user);
        }
        try {
            if (jwtUtil.isTokenExpiredOrInvalid(accessToken)) {
                return;
            }
            long expiry = jwtUtil.extractExpiration(accessToken);
            long ttl = expiry - System.currentTimeMillis();
            if (ttl > 0) {
                stringRedisTemplate.opsForValue().set(
                        "blacklist:token:" + accessToken,
                        "logout",
                        ttl,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            log.warn("Logout token blacklist failed: {}", e.getMessage());
        }
    }
    public AuthResponse exchangeOAuthCode(String code) {
        String redisKey = "oauth:exchange:" + code;
        String data = stringRedisTemplate.opsForValue().get(redisKey);

        if (data == null) {
            throw new AuthException("Invalid or expired exchange code");
        }
        stringRedisTemplate.delete(redisKey);

        try {
            Map<String,String> tokenData = objectMapper.readValue(data, Map.class);
            return AuthResponse.builder()
                    .accessToken(tokenData.get("accessToken"))
                    .refreshToken(tokenData.get("refreshToken"))
                    .tokenType("Bearer")
                    .expiresIn(jwtUtil.getJwtExpiration())
                    .userId(UUID.fromString(tokenData.get("userId")))
                    .name(tokenData.get("name"))
                    .email(tokenData.get("email"))
                    .role(tokenData.get("role"))
                    .build();
        } catch (Exception e) {
            throw new AuthException("Failed to process exchange code");
        }
    }
}
