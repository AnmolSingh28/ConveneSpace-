package com.concertbooking.concert_booking.review.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizerReviewResponse(
        UUID reviewId,
        String reviewerName,
        String concertTitle,
        Integer rating,
        String reviewText,
        Boolean wouldAttendAgain,
        LocalDateTime createdAt
) {
}
