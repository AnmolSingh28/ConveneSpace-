package com.concertbooking.concert_booking.booking.dto;

import java.math.BigDecimal;

public record OrganizerAnalyticsResponse(
        long totalEvents,
        long totalBookings,
        long totalTicketsSold,
        BigDecimal totalRevenue,
        double averageRating,
        long totalReviews,
        double attendanceRate
) {
}
