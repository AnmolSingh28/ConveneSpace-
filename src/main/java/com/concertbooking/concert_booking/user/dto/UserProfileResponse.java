package com.concertbooking.concert_booking.user.dto;

import com.concertbooking.concert_booking.common.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String phone,
        String email,
        UserRole role,
        String oauthProvider,
        boolean emailVerified,
        boolean phoneVerified,
        boolean active,
        LocalDateTime createdAt
) {
}
