package com.concertbooking.concert_booking.review.dto;

public record RatingDistributionResponse(
        Integer rating,
        Long count
) {
}
