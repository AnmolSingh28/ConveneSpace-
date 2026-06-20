package com.concertbooking.concert_booking.controllers;
import com.concertbooking.concert_booking.auth.filter.JwtAuthFilter;
import com.concertbooking.concert_booking.auth.oauth2.OAuth2SuccessHandler;
import com.concertbooking.concert_booking.auth.service.CustomUserDetailsService;
import com.concertbooking.concert_booking.auth.util.JwtUtil;
import com.concertbooking.concert_booking.common.enums.PaymentStatus;
import com.concertbooking.concert_booking.config.RateLimiting;
import com.concertbooking.concert_booking.payment.controller.PaymentController;
import com.concertbooking.concert_booking.payment.dto.CreatePaymentRequest;
import com.concertbooking.concert_booking.payment.dto.PaymentResponse;
import com.concertbooking.concert_booking.payment.service.PaymentService;
import com.concertbooking.concert_booking.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(
        controllers = PaymentController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")

public class PaymentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean private JwtAuthFilter jwtAuthFilter;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockBean private RateLimiting rateLimiting;
    private User user;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@test.com")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of()
                )
        );

        paymentResponse = new PaymentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CB-12345",
                BigDecimal.valueOf(1000),
                PaymentStatus.INITIATED,
                "order_123",
                null,
                "rzp_test_key",
                "INR",
                null,
                null,
                null,
                null,
                1,
                LocalDateTime.now()
        );
    }
    @Test
    void createOrder_success_returns200() throws Exception {

        CreatePaymentRequest request =
                new CreatePaymentRequest();

        request.setBookingId(UUID.randomUUID());

        when(paymentService.createOrder(
                any(CreatePaymentRequest.class),
                any(UUID.class)
        )).thenReturn(paymentResponse);

        mockMvc.perform(
                        post("/api/v1/payments/create-order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Payment order created successfully"))
                .andExpect(jsonPath("$.data.razorpayOrderId")
                        .value("order_123"));
    }
    @Test
    void createOrder_invalidRequest_returns400() throws Exception {

        mockMvc.perform(
                        post("/api/v1/payments/create-order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }
    @Test
    void webhook_success_returns200() throws Exception {

        mockMvc.perform(
                        post("/api/v1/payments/webhook")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(
                                        "X-Razorpay-Signature",
                                        "test_signature"
                                )
                                .content("{\"event\":\"payment.captured\"}")
                )
                .andExpect(status().isOk());

        verify(paymentService)
                .handleWebhook(
                        anyString(),
                        eq("test_signature")
                );
    }
    @Test
    void getPaymentStatus_success_returns200() throws Exception {

        UUID bookingId = UUID.randomUUID();

        when(paymentService.getPaymentByBooking(
                eq(bookingId),
                any(UUID.class)
        )).thenReturn(paymentResponse);

        mockMvc.perform(
                        get("/api/v1/payments/booking/{bookingId}",
                                bookingId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingReference")
                        .value("CB-12345"));
    }
    @Test
    void simulateSuccess_returns200() throws Exception {

        UUID bookingId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/payments/simulate-success/{bookingId}",
                                bookingId)
                )
                .andExpect(status().isOk());

        verify(paymentService)
                .simulatePaymentSuccess(bookingId);
    }
}

