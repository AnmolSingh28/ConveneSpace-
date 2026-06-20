package com.concertbooking.concert_booking.venue.dto;

import com.concertbooking.concert_booking.common.enums.SectionType;
import com.concertbooking.concert_booking.common.enums.VenueType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class VenueResponse {
    private UUID id;
    private String name;
    private String city;
    private String address;
    private VenueType venueType;
    private Integer totalCapacity;
    private String googleMapsURL;
    private String locationDescription;
    private String layoutImageUrl;
    private boolean isActive;
    private List<SectionResponse> sections;
    private Double latitude;
    private Double longitude;
}
