package com.concertbooking.concert_booking.concert.dto;

import com.concertbooking.concert_booking.common.enums.EventType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ConcertRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Artist name is required")
    private String artistName;

    @NotBlank(message = "Description is needed")
    private String description;

    @NotNull(message = "Venue is required")
    private UUID venueId;

    @NotNull(message = "Concert date is required")
    @Future(message = "Concert date must be in future")
    private LocalDateTime concertDate;

    private LocalDateTime doorsOpenTime;

    @NotNull(message = "Sale start time is required")
    private LocalDateTime saleStartTime;

    private LocalDateTime saleEndTime;

    private String bannerImageUrl;

    private boolean requiresPreRegistration=false;

    private LocalDateTime preRegistrationStart;

    private LocalDateTime preRegistrationEnd;

    @NotNull(message = "Event category Id is required")
    private UUID categoryId;
}

