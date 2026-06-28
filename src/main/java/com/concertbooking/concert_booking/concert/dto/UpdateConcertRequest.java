package com.concertbooking.concert_booking.concert.dto;
import com.concertbooking.concert_booking.common.enums.EventType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
@Data
public class UpdateConcertRequest {
    @NotBlank private String title;
    @NotBlank private String artistName;
    @NotBlank private String description;
    @NotNull private UUID venueId;
    @NotNull private UUID categoryId;
    @NotNull private LocalDateTime concertDate;
    private LocalDateTime doorsOpenTime;
    @NotNull private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private String bannerImageUrl;
    private boolean requiresPreRegistration;
    private LocalDateTime preRegistrationStart;
    private LocalDateTime preRegistrationEnd;
}
