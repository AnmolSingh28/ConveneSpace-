package com.concertbooking.concert_booking.venue.dto;

import com.concertbooking.concert_booking.common.enums.VenueType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VenueRequest {
    @NotBlank(message = "Venue name is required")
    private String name;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Venue type is required")
    private VenueType venueType;

    @NotNull(message = "Capacity is required")
    @Min(value=1, message = "Capacity must be atleast 1")
    private  Integer totalCapacity;

    private String googleMapsURL;
    private String locationDescription;
    private String layoutImageUrl;
    private Double latitude;
    private Double longitude;
}
