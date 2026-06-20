package com.concertbooking.concert_booking.analytics.dto;

import java.math.BigDecimal;

public record DashboardMetricsResponse(
        BigDecimal totalRevenue,
        long totalBookings,
        long totalEventsHosted,
        double averageRating,
        long totalReviews,
        double attendanceRate
) {
}
