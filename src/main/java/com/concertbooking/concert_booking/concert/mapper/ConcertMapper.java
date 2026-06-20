package com.concertbooking.concert_booking.concert.mapper;

import com.concertbooking.concert_booking.concert.dto.ConcertResponse;
import com.concertbooking.concert_booking.concert.dto.ConcertSummaryResponse;
import com.concertbooking.concert_booking.concert.dto.TicketTierResponse;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.venue.dto.VenueResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ConcertMapper {
    @Mapping(source = "concert.category.name", target = "category")
    @Mapping(source = "concert.venue.name", target = "venueName")
    @Mapping(source = "concert.venue.city", target = "venueCity")
    @Mapping(source = "startingPrice", target = "startingPrice")
    ConcertSummaryResponse toSummary(
            Concert concert,
            BigDecimal startingPrice
    );

    @Mapping(source = "concert.id", target = "id")
    @Mapping(source = "concert.title", target = "title")
    @Mapping(source = "concert.description", target = "description")
    @Mapping(source = "concert.artistName", target = "artistName")
    @Mapping(source = "concert.bannerImageUrl", target = "bannerImageUrl")
    @Mapping(source = "concert.concertDate", target = "concertDate")
    @Mapping(source = "concert.saleStartTime", target = "saleStartTime")
    @Mapping(source = "concert.saleEndTime", target = "saleEndTime")
    @Mapping(source = "concert.status", target = "status")
    @Mapping(source = "tiers", target = "ticketTiers")
    @Mapping(source = "venue", target = "venue")
    @Mapping(source = "concert.organizer.id", target = "organizerId")
    @Mapping(source = "concert.organizer.name", target = "organizerName")
    @Mapping(target = "organizerRating", ignore = true)
    @Mapping(source = "concert.category.name", target = "category")
    ConcertResponse toResponse(
            Concert concert,
            List<TicketTierResponse> tiers,
            VenueResponse venue
    );

    @Mapping(source = "section.id", target = "sectionId")
    @Mapping(source = "section.name", target = "sectionName")
    @Mapping(source = "section.sectionType",target = "sectionType")
    TicketTierResponse toTierResponse(TicketTier tier);
    }

