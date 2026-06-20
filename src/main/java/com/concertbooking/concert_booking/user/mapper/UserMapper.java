package com.concertbooking.concert_booking.user.mapper;

import com.concertbooking.concert_booking.user.dto.UserProfileResponse;
import com.concertbooking.concert_booking.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(source = "phoneVerification",target="phoneVerified")
    UserProfileResponse toResponse(User user);
}
