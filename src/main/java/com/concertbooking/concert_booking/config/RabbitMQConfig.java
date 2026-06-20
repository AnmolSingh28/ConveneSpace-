package com.concertbooking.concert_booking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // Queue Names
    public static final String BOOKING_CONFIRMATION_QUEUE= "booking.confirmation";
    public static final String BOOKING_CANCELLED_QUEUE= "booking.cancelled";
    public static final String PAYMENT_FAILED_QUEUE= "payment.failed";
    public static final String WAITLIST_NOTIFY_QUEUE= "waitlist.notify";
    public static final String EVENT_CHANGED_QUEUE= "event.changed";
    public static final String QR_GENERATION_QUEUE= "qr.generation";
    public static final String ORG_ANALYTICS_PAYMENT_QUEUE = "organizer.analytics.payment";
    public static final String ORG_ANALYTICS_REVIEW_QUEUE = "organizer.analytics.review";
    public static final String ORG_ANALYTICS_CHECKIN_QUEUE = "organizer.analytics.checkin";
    //  Dead Letter Queue Names
    public static final String DLQ_BOOKING_CONFIRMATION="booking.confirmation.dlq";
    public static final String DLQ_PAYMENT_FAILED= "payment.failed.dlq";
    public static final String DLQ_WAITLIST_NOTIFY = "waitlist.notify.dlq";
    public static final String DLQ_QR_GENERATION= "qr.generation.dlq";
    public static final String DLQ_ORG_ANALYTICS_PAYMENT_QUEUE = "organizer.analytics.payment.dlq";
    public static final String DLQ_ORG_ANALYTICS_REVIEW_QUEUE = "organizer.analytics.review.dlq";
    public static final String DLQ_ORG_ANALYTICS_CHECKIN_QUEUE = "organizer.analytics.checkin.dlq";
    //  Exchange Names
    public static final String CONCERT_EXCHANGE  = "concert.exchange";
    public static final String DLQ_EXCHANGE = "concert.dlq.exchange";

    //  Exchanges
    @Bean
    public DirectExchange concertExchange() {
        return new DirectExchange(CONCERT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange dlqExchange() {
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    // Main Queues
    @Bean
    public Queue bookingConfirmationQueue() {
        return QueueBuilder.durable(BOOKING_CONFIRMATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_BOOKING_CONFIRMATION)
                .build();
    }

    @Bean
    public Queue bookingCancelledQueue() {
        return QueueBuilder.durable(BOOKING_CANCELLED_QUEUE).build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_PAYMENT_FAILED)
                .build();
    }

    @Bean
    public Queue waitlistNotifyQueue() {
        return QueueBuilder.durable(WAITLIST_NOTIFY_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_WAITLIST_NOTIFY)
                .build();
    }

    @Bean
    public Queue eventChangedQueue() {
        return QueueBuilder.durable(EVENT_CHANGED_QUEUE).build();
    }

    @Bean
    public Queue qrGenerationQueue() {
        return QueueBuilder.durable(QR_GENERATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_QR_GENERATION)
                .build();
    }
    @Bean
    public Queue organizerAnalyticsPaymentQueue() {
        return QueueBuilder
                .durable(ORG_ANALYTICS_PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key",DLQ_ORG_ANALYTICS_PAYMENT_QUEUE)
                .build();
    }
    @Bean
    public Queue organizerAnalyticsReviewQueue() {
        return QueueBuilder
                .durable(ORG_ANALYTICS_REVIEW_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key",DLQ_ORG_ANALYTICS_REVIEW_QUEUE)
                .build();
    }
    @Bean
    public Queue organizerAnalyticsCheckInQueue() {
        return QueueBuilder
                .durable(ORG_ANALYTICS_CHECKIN_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key",DLQ_ORG_ANALYTICS_CHECKIN_QUEUE)
                .build();
    }

    //  Dead Letter Queues
    @Bean
    public Queue dlqBookingConfirmation() {
        return QueueBuilder.durable(DLQ_BOOKING_CONFIRMATION).build();
    }

    @Bean
    public Queue dlqPaymentFailed() {
        return QueueBuilder.durable(DLQ_PAYMENT_FAILED).build();
    }

    @Bean
    public Queue dlqWaitlistNotify() {
        return QueueBuilder.durable(DLQ_WAITLIST_NOTIFY).build();
    }

    @Bean
    public Queue dlqQrGeneration() {
        return QueueBuilder.durable(DLQ_QR_GENERATION).build();
    }

    @Bean
    public Queue dlqOrgPaymentAnalytics() {
        return QueueBuilder.durable(DLQ_ORG_ANALYTICS_PAYMENT_QUEUE).build();
    }

    @Bean
    public Queue dlqOrgReviewAnalytics() {return QueueBuilder.durable(DLQ_ORG_ANALYTICS_REVIEW_QUEUE).build();}

    @Bean
    public Queue dlqOrgCheckInAnalytics() {
        return QueueBuilder.durable(DLQ_ORG_ANALYTICS_CHECKIN_QUEUE).build();
    }
    // Bindings

    @Bean
    public Binding bookingConfirmationBinding() {
        return BindingBuilder
                .bind(bookingConfirmationQueue())
                .to(concertExchange())
                .with(BOOKING_CONFIRMATION_QUEUE);
    }

    @Bean
    public Binding bookingCancelledBinding() {
        return BindingBuilder
                .bind(bookingCancelledQueue())
                .to(concertExchange())
                .with(BOOKING_CANCELLED_QUEUE);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder
                .bind(paymentFailedQueue())
                .to(concertExchange())
                .with(PAYMENT_FAILED_QUEUE);
    }

    @Bean
    public Binding waitlistNotifyBinding() {
        return BindingBuilder
                .bind(waitlistNotifyQueue())
                .to(concertExchange())
                .with(WAITLIST_NOTIFY_QUEUE);
    }

    @Bean
    public Binding eventChangedBinding() {
        return BindingBuilder
                .bind(eventChangedQueue())
                .to(concertExchange())
                .with(EVENT_CHANGED_QUEUE);
    }

    @Bean
    public Binding qrGenerationBinding() {
        return BindingBuilder
                .bind(qrGenerationQueue())
                .to(concertExchange())
                .with(QR_GENERATION_QUEUE);
    }
    @Bean
    public Binding organizerAnalyticsPaymentBinding() {
        return BindingBuilder
                .bind(organizerAnalyticsPaymentQueue())
                .to(concertExchange())
                .with(ORG_ANALYTICS_PAYMENT_QUEUE);
    }
    @Bean
    public Binding organizerAnalyticsReviewBinding() {
        return BindingBuilder
                .bind(organizerAnalyticsReviewQueue())
                .to(concertExchange())
                .with(ORG_ANALYTICS_REVIEW_QUEUE);
    }
    @Bean
    public Binding organizerAnalyticsCheckInBinding() {
        return BindingBuilder
                .bind(organizerAnalyticsCheckInQueue())
                .to(concertExchange())
                .with(ORG_ANALYTICS_CHECKIN_QUEUE);
    }

    //  DLQ Bindings
    @Bean
    public Binding dlqBookingConfirmationBinding() {
        return BindingBuilder
                .bind(dlqBookingConfirmation())
                .to(dlqExchange())
                .with(DLQ_BOOKING_CONFIRMATION);
    }

    @Bean
    public Binding dlqPaymentFailedBinding() {
        return BindingBuilder
                .bind(dlqPaymentFailed())
                .to(dlqExchange())
                .with(DLQ_PAYMENT_FAILED);
    }

    @Bean
    public Binding dlqWaitlistNotifyBinding() {
        return BindingBuilder
                .bind(dlqWaitlistNotify())
                .to(dlqExchange())
                .with(DLQ_WAITLIST_NOTIFY);
    }

    @Bean
    public Binding dlqQrGenerationBinding() {
        return BindingBuilder
                .bind(dlqQrGeneration())
                .to(dlqExchange())
                .with(DLQ_QR_GENERATION);
    }
    @Bean
    public Binding dlqOrgAnalyticsPaymentBinding() {
        return BindingBuilder
                .bind(dlqOrgPaymentAnalytics())
                .to(concertExchange())
                .with(DLQ_ORG_ANALYTICS_PAYMENT_QUEUE);
    }
    @Bean
    public Binding dlqOrgAnalyticsReviewBinding() {
        return BindingBuilder
                .bind(dlqOrgReviewAnalytics())
                .to(concertExchange())
                .with(DLQ_ORG_ANALYTICS_REVIEW_QUEUE);
    }
    @Bean
    public Binding dlqOrgAnalyticsCheckInBinding() {
        return BindingBuilder
                .bind(dlqOrgCheckInAnalytics())
                .to(concertExchange())
                .with(DLQ_ORG_ANALYTICS_CHECKIN_QUEUE);
    }


    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter=new Jackson2JsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template=new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
