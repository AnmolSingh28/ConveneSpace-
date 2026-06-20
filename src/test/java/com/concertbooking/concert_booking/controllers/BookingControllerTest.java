package com.concertbooking.concert_booking.controllers;
import com.concertbooking.concert_booking.auth.filter.JwtAuthFilter;
import com.concertbooking.concert_booking.auth.service.CustomUserDetailsService;
import com.concertbooking.concert_booking.auth.util.JwtUtil;
import com.concertbooking.concert_booking.booking.controller.BookingController;
import com.concertbooking.concert_booking.booking.dto.BookingRequest;
import com.concertbooking.concert_booking.booking.dto.BookingResponse;
import com.concertbooking.concert_booking.booking.service.BookingService;
import com.concertbooking.concert_booking.common.enums.BookingStatus;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(
        controllers = BookingController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class BookingControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private BookingService bookingService;
    @MockBean private JwtAuthFilter jwtAuthFilter;
    private User mockUser;
    private BookingResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@test.com")
                .build();

        // Inject user into security context so @AuthenticationPrincipal resolves
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockUser, null, List.of())
        );

        mockResponse = new BookingResponse(
                UUID.randomUUID(),
                "CB-ABC12345",
                BookingStatus.PENDING,
                UUID.randomUUID(),
                "Coldplay",
                "Coldplay",
                LocalDateTime.now().plusDays(30),
                "Test Venue",
                "Bhopal",
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(1080),
                List.of(),
                null,
                null,
                null,
                LocalDateTime.now()
        );
    }

    @Test
    void createBooking_success_returns201() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setTierId(UUID.randomUUID());
        request.setQuantity(2);
        request.setIdempotencyKey(UUID.randomUUID().toString());

        when(bookingService.createBooking(any(BookingRequest.class), any(User.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Booking created successfully"))
                .andExpect(jsonPath("$.data.bookingReference").value("CB-ABC12345"));

        verify(bookingService).createBooking(any(BookingRequest.class), any(User.class));
    }

    @Test
    void createBooking_invalidRequest_returns400() throws Exception {
        // Empty body @Valid should reject
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookingService);
    }

    @Test
    void getMyBookings_success_returns200() throws Exception {
        when(bookingService.getUserBookings(any(User.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockResponse)));

        mockMvc.perform(get("/api/v1/bookings/my-bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Bookings have been successfully fetched"));
    }

    @Test
    void getByReference_success_returns200() throws Exception {
        when(bookingService.getBookingByReference(eq("CB-ABC12345"), any(User.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/bookings/reference/CB-ABC12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingReference").value("CB-ABC12345"));
    }

    @Test
    void getByReference_notFound_returns404() throws Exception {
        when(bookingService.getBookingByReference(any(), any()))
                .thenThrow(new ResourceNotFoundException("Booking not found"));

        mockMvc.perform(get("/api/v1/bookings/reference/INVALID-REF"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_success_returns200() throws Exception {
        UUID bookingId = mockResponse.id();
        when(bookingService.getBookingById(eq(bookingId), any(User.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void cancelBooking_success_returns200() throws Exception {
        UUID bookingId = UUID.randomUUID();
        BookingResponse cancelled = new BookingResponse(
                bookingId, "CB-ABC12345", BookingStatus.CANCELLED,
                UUID.randomUUID(), "Coldplay", "Coldplay",
                LocalDateTime.now().plusDays(30), "Test Venue", "Bhopal",
                BigDecimal.valueOf(1000), BigDecimal.valueOf(50),
                BigDecimal.valueOf(30), BigDecimal.valueOf(1080),
                List.of(), LocalDateTime.now(), "Changed plans",
                BigDecimal.valueOf(1080), LocalDateTime.now()
        );

        when(bookingService.cancelBooking(eq(bookingId), any(User.class), any())).thenReturn(cancelled);

        mockMvc.perform(delete("/api/v1/bookings/{id}", bookingId)
                        .param("reason", "Changed plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.message").value("Booking has been cancelled successfully"));
    }

    @Test
    void cancelBooking_serviceThrows_returns404() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.cancelBooking(any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Booking not found"));

        mockMvc.perform(delete("/api/v1/bookings/{id}", bookingId))
                .andExpect(status().isNotFound());
    }
}
