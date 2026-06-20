package com.concertbooking.concert_booking.concert.dto;

import com.concertbooking.concert_booking.common.enums.SectionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketTierRequest {
    @NotBlank(message = "Tier name is required")
    private String tierName;

    @NotNull(message = "Section is required")
    private UUID sectionId;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer totalQuantity;

    @NotNull(message = "Max per user is required")
    @Min(value = 1, message = "Max per user must be at least 1")
    private Integer maxPerUser;

    private LocalDateTime saleStart;
    private LocalDateTime saleEnd;
    private Integer lockTtlMinutes=10;
    private Integer rowCount;
    private Integer seatsPerRow;
    private SectionType sectionType;
}
