package com.concertbooking.concert_booking.controllers;

import com.concertbooking.concert_booking.auth.filter.JwtAuthFilter;
import com.concertbooking.concert_booking.auth.oauth2.OAuth2SuccessHandler;
import com.concertbooking.concert_booking.auth.service.CustomUserDetailsService;
import com.concertbooking.concert_booking.auth.util.JwtUtil;
import com.concertbooking.concert_booking.config.RateLimiting;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.queue.controller.VirtualQueueController;
import com.concertbooking.concert_booking.queue.dto.QueueJoinResponse;
import com.concertbooking.concert_booking.queue.dto.QueuePositionResponse;
import com.concertbooking.concert_booking.queue.service.VirtualQueueService;
import com.concertbooking.concert_booking.user.entity.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(
        controllers = VirtualQueueController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class VirtualQueueControllerTest {
    @MockBean private VirtualQueueService virtualQueueService;
    @MockBean private TicketTierRepository ticketTierRepository;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private JwtAuthFilter jwtAuthFilter;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private OAuth2SuccessHandler oAuth2SuccessHandler;
    @MockBean private RateLimiting rateLimiting;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@test.com")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }
    @Test
    void joinQueue_success_returns200() throws Exception {

        UUID tierId = UUID.randomUUID();

        QueueJoinResponse response = new QueueJoinResponse(tierId,10,9,20,2,"Joined successfully");

        when(virtualQueueService.joinQueue(eq(tierId), any(User.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/queue/join/{tierId}", tierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Successfully joined queue"));
    }
    @Test
    void getPosition_success_returns200() throws Exception {

        UUID tierId = UUID.randomUUID();

        QueuePositionResponse response = new QueuePositionResponse(tierId, 5, 4, 20, 1);

        when(virtualQueueService.getPosition(eq(tierId),
                any(User.class)
        )).thenReturn(response);

        mockMvc.perform(get("/api/v1/queue/position/{tierId}", tierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Queue position fetched"));
    }
    @Test
    void leaveQueue_success_returns200() throws Exception {

        UUID tierId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/queue/leave/{tierId}", tierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Left queue successfully"));

        verify(virtualQueueService).leaveQueue(eq(tierId), any(User.class));
    }
    @Test
    void getQueueStatus_success_returns200() throws Exception {

        UUID tierId = UUID.randomUUID();

        when(virtualQueueService.getQueueSize(tierId)).thenReturn(20L);

        when(virtualQueueService.getRealAvailableQuantity(tierId)).thenReturn(100);

        when(virtualQueueService.isQueueActive(tierId)).thenReturn(true);

        mockMvc.perform(get("/api/v1/queue/status/{tierId}", tierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.queueSize")
                        .value(20));
    }
    @Test
    void getConcertIdByTier_success_returns200() throws Exception {

        UUID tierId = UUID.randomUUID();
        UUID concertId = UUID.randomUUID();

        Concert concert = Concert.builder().id(concertId).build();

        TicketTier tier = TicketTier.builder().id(tierId).concert(concert).build();

        when(ticketTierRepository.findById(tierId)).thenReturn(Optional.of(tier));

        mockMvc.perform(get("/api/v1/queue/concert-id/{tierId}", tierId)).andExpect(status().isOk());
    }
    @Test
    void getConcertIdByTier_notFound_returns404() throws Exception {

        UUID tierId = UUID.randomUUID();

        when(ticketTierRepository.findById(tierId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/queue/concert-id/{tierId}", tierId)).andExpect(status().isNotFound());
    }
    @Test
    void hasToken_success_returns200() throws Exception {

        UUID tierId = UUID.randomUUID();

        when(virtualQueueService.hasValidQueueToken(eq(tierId), any(User.class))).thenReturn(true);

        mockMvc.perform(
                        get("/api/v1/queue/has-token/{tierId}", tierId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data")
                        .value(true));
    }
}
