package com.concertbooking.concert_booking.auth.dto;

import com.concertbooking.concert_booking.common.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message ="Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain uppercase, lowercase, and a number"
    )
    private String password;

    @Pattern(regexp = "(^$|^[6-9]\\d{9}$)", message = "Invalid Indian phone number")
    private String phone;

    @NotNull(message = "Role is required")
    private UserRole role;
}
