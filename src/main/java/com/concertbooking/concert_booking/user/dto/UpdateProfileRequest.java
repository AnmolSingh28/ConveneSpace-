package com.concertbooking.concert_booking.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min=2,max = 100)
    private String name;

    @Pattern(regexp = "^[6-9]\\d{9}$",message = "Phone number should not exceed 10 characters")
    private String phone;
}
