package com.concertbooking.concert_booking.booking.mapper;

import com.concertbooking.concert_booking.booking.dto.BookingItemResponse;
import com.concertbooking.concert_booking.booking.dto.BookingResponse;
import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    @Mapping(source = "concert.id", target = "concertId")
    @Mapping(source = "concert.title", target = "concertTitle")
    @Mapping(source = "concert.artistName", target = "artistName")
    @Mapping(source = "concert.concertDate", target = "concertDate")
    @Mapping(source = "concert.venue.name", target = "venueName")
    @Mapping(source = "concert.venue.city", target = "venueCity")
    BookingResponse toResponse(Booking booking);

    @Mapping(source = "tier.tierName", target = "tierName")
    @Mapping(source = "tier.section.name", target = "sectionName")
    @Mapping(source = "seatInventory.rowLabel", target = "rowLabel")
    @Mapping(source = "seatInventory.seatNumber", target = "seatNumber")
    BookingItemResponse toItemResponse(BookingItem item);
}
