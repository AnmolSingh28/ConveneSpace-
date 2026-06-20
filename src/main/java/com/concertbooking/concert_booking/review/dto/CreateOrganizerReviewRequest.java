package com.concertbooking.concert_booking.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrganizerReviewRequest(
        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @NotBlank
        String reviewText,

        @NotNull
        Boolean wouldAttendAgain

) {
}