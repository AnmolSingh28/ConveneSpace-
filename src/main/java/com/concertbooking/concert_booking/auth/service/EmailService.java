package com.concertbooking.concert_booking.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sendinblue.ApiClient;
import sendinblue.Configuration;
import sibApi.TransactionalEmailsApi;
import sibModel.SendSmtpEmail;
import sibModel.SendSmtpEmailSender;
import sibModel.SendSmtpEmailTo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name}")
    private String fromName;

    private TransactionalEmailsApi buildApi() {
        ApiClient client = Configuration.getDefaultApiClient();
        client.setApiKey(apiKey);
        return new TransactionalEmailsApi(client);
    }

    @Async
    public void sendOtpEmail(String toEmail, String userName, String otp) {
        try {
            SendSmtpEmail email = new SendSmtpEmail();
            email.setSender(new SendSmtpEmailSender().name(fromName).email(fromEmail));
            email.setTo(List.of(new SendSmtpEmailTo().email(toEmail).name(userName)));
            email.setSubject("Your ConcertBooking Verification Code");
            email.setTextContent(
                    "Hi " + userName + ",\n\n" +
                            "Your verification code is: " + otp + "\n\n" +
                            "This code expires in 10 minutes.\n" +
                            "Do not share this code with anyone.\n\n" +
                            "ConcertBooking Team"
            );
            log.info("Attempting Brevo send to {}", toEmail);
            log.info("Generated OTP is: {}", otp);
            buildApi().sendTransacEmail(email);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {

            if (e instanceof sendinblue.ApiException apiException){
                log.error("Brevo status code: {}",apiException.getCode());
                log.error("Brevo response body: {}",apiException.getResponseBody());
            }
            log.error("Failed to send OTP email to {}", toEmail, e);
        }
    }

    @Async
    public void sendBookingConfirmationEmail(String toEmail,String userName,String bookingRef,String concertTitle){
        try{
            SendSmtpEmail email = new SendSmtpEmail();
            email.setSender(new SendSmtpEmailSender().name(fromName).email(fromEmail));
            email.setTo(List.of(new SendSmtpEmailTo().email(toEmail).name(userName)));
            email.setSubject("Booking Confirmed — " + concertTitle);
            email.setTextContent(
                    "Hi " + userName + ",\n\n" +
                            "Your booking is confirmed!\n" +
                            "Booking Reference: " + bookingRef + "\n" +
                            "Concert: " + concertTitle + "\n\n" +
                            "Your QR ticket will be emailed separately.\n\n" +
                            "ConcertBooking Team"
            );
            buildApi().sendTransacEmail(email);
            log.info("Booking confirmation sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send booking confirmation to {}: {}", toEmail, e.getMessage());
        }
    }
        @Async
        public void sendBookingCancellationEmail(String toEmail,String userName,String bookingRef,String concertTitle,BigDecimal refundAmount){
            try{
                SendSmtpEmail email = new SendSmtpEmail();
                email.setSender(new SendSmtpEmailSender().name(fromName).email(fromEmail));
                email.setTo(List.of(new SendSmtpEmailTo().email(toEmail).name(userName)));
                email.setSubject("Booking Cancelled — " + concertTitle);
                email.setTextContent(
                        "Hi " + userName + ",\n\n" +
                                "Your booking has been cancelled.\n" +
                                "Booking Reference: " + bookingRef + "\n" +
                                "Concert: " + concertTitle + "\n" +
                                "Refund Amount: ₹" + refundAmount + "\n\n" +
                                "Refund will be processed within 5-7 business days.\n\n" +
                                "ConcertBooking Team"
                );
                buildApi().sendTransacEmail(email);
                log.info("Cancellation email sent to: {}", toEmail);
            } catch (Exception e) {
                log.error("Failed to send cancellation email to {}: {}", toEmail, e.getMessage());
            }
        }
        @Async
    public void sendQrTicketEmail(String toEmail,String userName,String bookingRef,String concertTitle){
        try{
            SendSmtpEmail email = new SendSmtpEmail();
            email.setSender(new SendSmtpEmailSender().name(fromName).email(fromEmail));
            email.setTo(List.of(new SendSmtpEmailTo().email(toEmail).name(userName)));
            email.setSubject("Your tickets " + concertTitle);
            email.setHtmlContent(
                    "<h2>Hi " + userName + ",</h2>" +
                            "<p>Your tickets for <strong>" + concertTitle +
                            "</strong> are confirmed!</p>" +
                            "<p><strong>Booking Reference:</strong> " + bookingRef + "</p>" +
                            "<p>Your QR code is ready. Open the app to view and download it:</p>" +
                            "<a href=\"http://localhost:5173/my-bookings\" " +
                            "style=\"background:#3b82f6;color:white;padding:12px 24px;" +
                            "border-radius:8px;text-decoration:none;display:inline-block;" +
                            "margin:16px 0\">View My Tickets</a>" +
                            "<p style=\"color:#666;font-size:13px\">Show the QR code " +
                            "from the app at the venue entrance.</p>" +
                            "<br/><p>ConveneSpace Team</p>"
            );
            buildApi().sendTransacEmail(email);
            log.info("QR ticket email sent to: {}", toEmail);

        }catch (Exception e){
            log.error("Failed to send QR email to {}: {}",toEmail,e.getMessage());
        }
        }

    }

