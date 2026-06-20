package com.concertbooking.concert_booking.seat.mapper;


import com.concertbooking.concert_booking.seat.dto.SeatResponse;
import com.concertbooking.concert_booking.seat.entity.SeatInventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SeatMapper {
    @Mapping(source = "tier.id",target = "tierId")
    @Mapping(source = "tier.tierName",target = "tierName")
    @Mapping(source = "tier.price",target = "price")
    SeatResponse toResponse(SeatInventory seat);
    List<SeatResponse> toResponseList(List<SeatInventory>seats);
}
