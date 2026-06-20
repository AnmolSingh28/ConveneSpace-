package com.concertbooking.concert_booking.venue.dto;

import com.concertbooking.concert_booking.common.enums.SectionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data

public class SectionRequest {
    @NotBlank(message = "Section name is required")
    private String name;

    @NotNull(message = "Section type is required")
    private SectionType sectionType;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer totalCapacity;

    private Float xPosition;
    private Float yPosition;
    private Float width;
    private Float height;
    private String colorHex;
}
