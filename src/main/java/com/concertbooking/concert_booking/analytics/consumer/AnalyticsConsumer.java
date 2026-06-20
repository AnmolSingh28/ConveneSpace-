package com.concertbooking.concert_booking.analytics.consumer;

import com.concertbooking.concert_booking.analytics.entity.OrganizerAnalytics;
import com.concertbooking.concert_booking.analytics.repository.OrganizerAnalyticsRepository;
import com.concertbooking.concert_booking.config.RabbitMQConfig;
import com.concertbooking.concert_booking.payment.event.PaymentSuccessEvent;
import com.concertbooking.concert_booking.review.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsConsumer {
    private final OrganizerAnalyticsRepository organizerAnalyticsRepository;
    @Transactional
    @RabbitListener(queues = RabbitMQConfig.ORG_ANALYTICS_PAYMENT_QUEUE)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        OrganizerAnalytics analytics=organizerAnalyticsRepository.findByOrganizerId(event.getOrganizerId())
                        .orElseGet(()->OrganizerAnalytics.builder()
                                .organizerId(event.getOrganizerId())
                                .totalRevenue(BigDecimal.ZERO)
                                .totalBookings(0)
                                .totalEvents(0)
                                .totalReviews(0)
                                .totalTicketsSold(0)
                                .checkedInCount(0)
                                .averageRating(0.0)
                                .attendanceRate(0.0)
                                .build()
                        );

        analytics.setTotalBookings(analytics.getTotalBookings()+1);

        analytics.setTotalRevenue(analytics.getTotalRevenue().add(event.getAmount()));

        organizerAnalyticsRepository.save(analytics);
        log.info("Analytics updated. organizerId={} revenue={} bookings={}",
                event.getOrganizerId(),
                analytics.getTotalRevenue(),
                analytics.getTotalBookings()
        );
    }
    @Transactional
    @RabbitListener(queues = RabbitMQConfig.ORG_ANALYTICS_REVIEW_QUEUE)
    public void handleReviewCreated(
            ReviewCreatedEvent event
    ) {
        OrganizerAnalytics analytics = organizerAnalyticsRepository.findByOrganizerId(event.getOrganizerId()).orElseThrow();

        long oldReviews = analytics.getTotalReviews();
        double oldAverage = analytics.getAverageRating();

        double newAverage = ((oldAverage * oldReviews) + event.getRating()) / (oldReviews + 1);

        analytics.setTotalReviews(oldReviews + 1);
        analytics.setAverageRating(newAverage);

        organizerAnalyticsRepository.save(analytics);
    }
    @Transactional
    @RabbitListener(queues=RabbitMQConfig.ORG_ANALYTICS_CHECKIN_QUEUE)
    public void handleCheckin(String organizerId) {
        OrganizerAnalytics analytics=organizerAnalyticsRepository.findByOrganizerId(UUID.fromString(organizerId))
                .orElseThrow();

        analytics.setCheckedInCount(analytics.getCheckedInCount()+1);
        if (analytics.getTotalBookings()>0) {
            double rate=(double) analytics.getCheckedInCount()/analytics.getTotalBookings() * 100;
            analytics.setAttendanceRate(rate);
        }

        organizerAnalyticsRepository.save(analytics);
        log.info("Checkin analytics updated. organizerId={} checkedIn={} rate={}%",organizerId,analytics.getCheckedInCount(),analytics.getAttendanceRate());
    }
}
