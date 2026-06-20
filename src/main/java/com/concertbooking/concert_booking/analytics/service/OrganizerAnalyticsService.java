package com.concertbooking.concert_booking.analytics.service;

import com.concertbooking.concert_booking.analytics.dto.DashboardMetricsResponse;
import com.concertbooking.concert_booking.analytics.entity.OrganizerAnalytics;
import com.concertbooking.concert_booking.analytics.repository.OrganizerAnalyticsRepository;
import com.concertbooking.concert_booking.concert.repository.ConcertRepository;
import com.concertbooking.concert_booking.review.repository.OrganizerReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizerAnalyticsService{
    private final OrganizerAnalyticsRepository analyticsRepository;
    private final ConcertRepository concertRepository;

    public DashboardMetricsResponse getDashboardMetrics(UUID organizerId){

        OrganizerAnalytics analytics = analyticsRepository.findByOrganizerId(organizerId)
                .orElse(
                        OrganizerAnalytics.builder()
                                .totalRevenue(java.math.BigDecimal.ZERO)
                                .totalBookings(0)
                                .totalEvents(0)
                                .totalReviews(0)
                                .averageRating(0.0)
                                .attendanceRate(0.0)
                                .build()
                );

        long totalEvents = concertRepository.countByOrganizerId(organizerId);

        return new DashboardMetricsResponse(
                analytics.getTotalRevenue(),
                analytics.getTotalBookings(),
                totalEvents,
                analytics.getAverageRating(),
                analytics.getTotalReviews(),
                analytics.getAttendanceRate()
        );
    }
}
