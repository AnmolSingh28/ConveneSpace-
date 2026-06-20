package com.concertbooking.concert_booking.auth.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExchangeCodeRequest {
    @NotBlank
    private String code;
}
