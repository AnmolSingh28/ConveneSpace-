package com.concertbooking.concert_booking.concert.mapper;

import com.concertbooking.concert_booking.concert.dto.PreRegistrationResponse;
import com.concertbooking.concert_booking.concert.entity.ConcertPreRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PreRegistrationMapper {
    @Mapping(source = "concert.id",target = "concertId")
    @Mapping(source = "concert.title",target = "concertTitle")
    PreRegistrationResponse toResponse(ConcertPreRegistration registration);
}
