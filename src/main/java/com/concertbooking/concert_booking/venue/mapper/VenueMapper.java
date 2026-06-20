package com.concertbooking.concert_booking.venue.mapper;

import com.concertbooking.concert_booking.venue.dto.SectionResponse;
import com.concertbooking.concert_booking.venue.dto.VenueResponse;
import com.concertbooking.concert_booking.venue.entity.Venue.Venue;
import com.concertbooking.concert_booking.venue.entity.Venue.VenueSection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface VenueMapper {
    @Mapping(source = "active", target = "isActive")
    VenueResponse toResponse(Venue venue);

    List<VenueResponse> toResponseList(List<Venue> venues);

    @Mapping(source = "active", target = "isActive")
    SectionResponse toSectionResponse(VenueSection section);

    List<SectionResponse> toSectionResponseList(List<VenueSection> sections);

}
