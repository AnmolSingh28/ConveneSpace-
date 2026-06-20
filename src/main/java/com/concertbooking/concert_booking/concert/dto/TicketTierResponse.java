package com.concertbooking.concert_booking.concert.dto;

import com.concertbooking.concert_booking.common.enums.SectionType;
import com.concertbooking.concert_booking.common.enums.TierStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TicketTierResponse {
    private UUID id;
    private String tierName;
    private UUID sectionId;
    private String sectionName;
    private BigDecimal price;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private Integer maxPerUser;
    private LocalDateTime saleStart;
    private LocalDateTime saleEnd;
    private TierStatus tierStatus;
    private Integer lockTtlMinutes;
    private SectionType sectionType;

}
