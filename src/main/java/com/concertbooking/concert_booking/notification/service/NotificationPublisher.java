package com.concertbooking.concert_booking.notification.service;

import com.concertbooking.concert_booking.booking.event.BookingCancelledEvent;
import com.concertbooking.concert_booking.config.RabbitMQConfig;
import com.concertbooking.concert_booking.payment.event.PaymentFailedEvent;
import com.concertbooking.concert_booking.payment.event.PaymentSuccessEvent;
import com.concertbooking.concert_booking.review.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishBookingConfirmation(PaymentSuccessEvent event){
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CONCERT_EXCHANGE, //exchange name
                RabbitMQConfig.BOOKING_CONFIRMATION_QUEUE, //routing id
                event //msg itself
        );
        log.info("Queued booking confirmation for: {}",
                event.getBookingReference());
    }
    public void publishBookingCancelled(BookingCancelledEvent event){
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CONCERT_EXCHANGE,
                RabbitMQConfig.BOOKING_CANCELLED_QUEUE,
                event
        );
        log.info("Queued booking confirmation for: {}",
                event.getBookingReference());
    }
    public void publishQrGeneration(PaymentSuccessEvent event){
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CONCERT_EXCHANGE,
                RabbitMQConfig.QR_GENERATION_QUEUE,
                event
        );
        log.info("Queued booking confirmation for: {}",
                event.getBookingReference());
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CONCERT_EXCHANGE,
                RabbitMQConfig.PAYMENT_FAILED_QUEUE,
                event
        );
        log.info("Queued payment failed notification for: {}",
                event.getBookingReference());
    }
    public void publishAnalyticsUpdate(PaymentSuccessEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CONCERT_EXCHANGE,
                RabbitMQConfig.ORG_ANALYTICS_PAYMENT_QUEUE,
                event
        );
    }

    public void publishReviewCreated(ReviewCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CONCERT_EXCHANGE,
                RabbitMQConfig.ORG_ANALYTICS_REVIEW_QUEUE,
                event
        );
    }
    public void publishCheckinEvent(UUID organizerId) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CONCERT_EXCHANGE,
                RabbitMQConfig.ORG_ANALYTICS_CHECKIN_QUEUE,
                organizerId.toString()
        );
    }



}
