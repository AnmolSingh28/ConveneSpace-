package com.concertbooking.concert_booking.review.dto;

import java.util.List;

public record OrganizerRatingSummaryResponse(
        String organizerName,
        Double averageRating,
        Long totalReviews,
        List<RatingDistributionResponse> distribution
) {
}
