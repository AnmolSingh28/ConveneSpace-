

package com.concertbooking.concert_booking.notification.listener;

import com.concertbooking.concert_booking.auth.service.EmailService;
import com.concertbooking.concert_booking.booking.entity.Booking;
import com.concertbooking.concert_booking.booking.entity.BookingItem;
import com.concertbooking.concert_booking.booking.repository.BookingItemRepository;
import com.concertbooking.concert_booking.booking.repository.BookingRepository;
import com.concertbooking.concert_booking.common.enums.BookingStatus;
import com.concertbooking.concert_booking.config.RabbitMQConfig;
import com.concertbooking.concert_booking.payment.event.PaymentSuccessEvent;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class QrGenerationListener {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final EmailService emailService;

    @RabbitListener(
            queues = RabbitMQConfig.QR_GENERATION_QUEUE,
            ackMode = "MANUAL"
    )
    public void handleQrGeneration(PaymentSuccessEvent event,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long tag)
            throws IOException {
        try {
            log.info("Generating QR codes for booking: {}", event.getBookingReference());

            Booking booking = bookingRepository.findById(event.getBookingId())
                    .orElseThrow(() -> new RuntimeException(
                            "Booking not found: " + event.getBookingId()));

            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                log.warn("Skipping QR generation — booking not confirmed: {}",
                        event.getBookingReference());
                channel.basicAck(tag, false);
                return;
            }

            List<BookingItem> items = bookingItemRepository
                    .findByBookingIdIn(List.of(event.getBookingId()));



            for (BookingItem item : items) {
                String qrToken = generateQrToken();
                String qrContent = buildQrContent(
                        booking.getBookingReference(),
                        item.getId(),
                        qrToken
                );

                String qrBase64 = generateQrImage(qrContent);

                item.setQrToken(qrToken);
                item.setQrTokenExpiresAt(LocalDateTime.now().plusHours(24));
                item.setQrCodeUrl(qrBase64);

            }

            bookingItemRepository.saveAll(items);

                emailService.sendQrTicketEmail(
                        event.getUserEmail(),
                        event.getUserName(),
                        event.getBookingReference(),
                        event.getConcertTitle()

                );

            channel.basicAck(tag, false);
            log.info("QR codes generated for {} items in booking: {}",
                    items.size(), event.getBookingReference());

        } catch (Exception e) {
            log.error("QR generation failed for booking: {} — {}",
                    event.getBookingReference(), e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }

    private String generateQrToken() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String buildQrContent(String bookingRef, UUID itemId, String token) {
        return String.format("CONVENE|%s|%s|%s", bookingRef, itemId, token);
    }

    private String generateQrImage(String content) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(
                content,
                BarcodeFormat.QR_CODE,
                300, 300,
                Map.of(EncodeHintType.MARGIN, 2)
        );

        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        byte[] bytes = out.toByteArray();

        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}