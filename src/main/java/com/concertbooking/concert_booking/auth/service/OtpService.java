package com.concertbooking.concert_booking.auth.service;

import com.concertbooking.concert_booking.common.exception.OtpException;
import io.jsonwebtoken.security.Password;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.otp.max-attempts}")
    private int maxAttempts;

    @Value("${app.otp.length}")
    private int otpLength;

    private static final String OTP_PREFIX = "otp:";
    private static final String ATTEMPTS_PREFIX = "otp:attempts:";

    public String generateAndStoreOtp(String email) {
        String otp = generateOtp();

        // Store OTP with TTL
        String key = OTP_PREFIX + email;
        String hashedOtp= passwordEncoder.encode(otp);
        redisTemplate.opsForValue().set(key, hashedOtp, otpExpiryMinutes, TimeUnit.MINUTES);

        // Reset attempt counter
        redisTemplate.delete(ATTEMPTS_PREFIX + email);

        log.info("OTP generated for email: {}", email);
        return otp;
    }

    public void verifyOtp(String email, String inputOtp) {
        String attemptsKey = ATTEMPTS_PREFIX + email;


        Object attemptsObj = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsObj != null ? Integer.parseInt(attemptsObj.toString()) : 0;

        if (attempts >= maxAttempts) {
            throw new OtpException("Too many failed attempts. Request a new OTP.");
        }

        String key = OTP_PREFIX + email;
        Object storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            throw new OtpException("OTP expired or not found. Please request a new one.");
        }

        if (!passwordEncoder.matches(inputOtp,storedOtp.toString())) {

            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, otpExpiryMinutes, TimeUnit.MINUTES);
            throw new OtpException("Invalid OTP. " + (maxAttempts - attempts - 1) + " attempts remaining.");
        }


        redisTemplate.delete(key);
        redisTemplate.delete(attemptsKey);
        log.info("OTP verified successfully for: {}", email);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
}
