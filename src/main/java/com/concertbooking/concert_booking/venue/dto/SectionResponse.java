package com.concertbooking.concert_booking.venue.dto;

import com.concertbooking.concert_booking.common.enums.SectionType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SectionResponse {
    private UUID id;
    private String name;
    private SectionType sectionType;
    private Integer totalCapacity;
    private Float xPosition;
    private Float yPosition;
    private Float width;
    private Float height;
    private String colorHex;
    private boolean isActive;
}
