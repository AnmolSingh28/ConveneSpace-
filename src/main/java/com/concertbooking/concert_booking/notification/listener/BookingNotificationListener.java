package com.concertbooking.concert_booking.notification.listener;


import com.concertbooking.concert_booking.booking.event.BookingCancelledEvent;
import com.concertbooking.concert_booking.auth.service.EmailService;
import com.concertbooking.concert_booking.config.RabbitMQConfig;
import com.concertbooking.concert_booking.payment.event.PaymentSuccessEvent;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingNotificationListener {
    private final EmailService emailService;

    @RabbitListener(
            queues = RabbitMQConfig.BOOKING_CONFIRMATION_QUEUE,
            ackMode = "MANUAL"
    )
    public void handleBookingConfirmation(
          PaymentSuccessEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            log.info("Booking confirmation email sent for: {}",
                    event.getBookingReference());
            emailService.sendBookingConfirmationEmail(
                    event.getUserEmail(),
                    event.getUserName(),
                    event.getBookingReference(),
                    event.getConcertTitle()
            );

            channel.basicAck(tag, false);
            log.info("Booking confirmation email sent for: {}",
                    event.getBookingReference());
        } catch (Exception ex) {
            log.error("Failed to process booking confirmation for: {} — {}",
                    event.getBookingReference(), ex.getMessage());
            // Nack means requeue false this means that it will go to DLQ, when any failure happens
            channel.basicNack(tag, false, false);
        }
    }

    @RabbitListener(
            queues = RabbitMQConfig.BOOKING_CANCELLED_QUEUE,
            ackMode = "MANUAL"
    )
    public void handleBookingCancellation(
            BookingCancelledEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            log.info("Processing cancellation notification for: {}",
                    event.getBookingReference());
            emailService.sendBookingCancellationEmail(
                    event.getUserEmail(),
                    event.getUserName(),
                    event.getBookingReference(),
                    event.getConcertTitle(),
                    event.getRefundAmount()

            );
            channel.basicAck(tag,false);

        } catch (Exception ex){
         log.error("Failed to process notifications for cancellation: {}",
                 ex.getMessage());
         channel.basicNack(tag,false,false);
        }

    }


}
