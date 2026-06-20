package com.concertbooking.concert_booking.concert.mapper;
import com.concertbooking.concert_booking.concert.dto.EventCategoryResponse;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface EventCategoryMapper {
    EventCategoryResponse toResponse(EventCategory category);
}
