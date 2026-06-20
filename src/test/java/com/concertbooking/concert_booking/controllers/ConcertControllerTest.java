package com.concertbooking.concert_booking.controllers;

import com.concertbooking.concert_booking.auth.filter.JwtAuthFilter;
import com.concertbooking.concert_booking.auth.oauth2.OAuth2SuccessHandler;
import com.concertbooking.concert_booking.auth.service.CustomUserDetailsService;
import com.concertbooking.concert_booking.auth.util.JwtUtil;
import com.concertbooking.concert_booking.common.enums.*;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.concert.controller.ConcertController;
import com.concertbooking.concert_booking.concert.dto.*;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import com.concertbooking.concert_booking.concert.repository.EventCategoryRepository;
import com.concertbooking.concert_booking.concert.service.ConcertService;
import com.concertbooking.concert_booking.concert.service.PreRegistrationService;
import com.concertbooking.concert_booking.common.response.PagedResponse;
import com.concertbooking.concert_booking.config.RateLimiting;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
        controllers = ConcertController.class,
        excludeAutoConfiguration = {OAuth2ClientAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class ConcertControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private ConcertService concertService;
    @MockBean  private PreRegistrationService preRegistrationService;
    @MockBean private JwtAuthFilter jwtAuthFilter;
    @MockBean private EventCategoryRepository eventCategoryRepository;

    @MockBean
    private RateLimiting rateLimiting;
    private User mockUser;
    private ConcertResponse mockConcertResponse;
    private ConcertSummaryResponse mockSummary;
    private EventCategory testCategory;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test Organizer")
                .email("organizer@test.com")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        mockUser, null,
                        List.of(new SimpleGrantedAuthority("ROLE_ORGANIZER"))
                )
        );
        testCategory = EventCategory.builder().id(UUID.randomUUID()).name("Live Music")
                .active(true).build();

        mockSummary = new ConcertSummaryResponse(
                UUID.randomUUID(), "Coldplay", "Coldplay",
                null, "Test Venue", "Bhopal",
                LocalDateTime.now().plusDays(30),
                LocalDateTime.now().plusDays(1),
                ConcertStatus.PUBLISHED, false, false,
                BigDecimal.valueOf(1000),
                "Live music",
                null,null,null
        );

        mockConcertResponse = ConcertResponse.builder()
                .id(UUID.randomUUID())
                .title("Coldplay")
                .artistName("Coldplay")
                .description("Test")
                .status(ConcertStatus.PUBLISHED)
                .concertDate(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Test
    void getUpcomingConcerts_success_returns200() throws Exception {
        PagedResponse<ConcertSummaryResponse> paged = PagedResponse.<ConcertSummaryResponse>builder()
                        .content(List.of(mockSummary))
                        .page(0).size(10).totalElements(1).totalPages(1).first(true).last(true)
                        .build();
        when(concertService.getUpcomingConcerts(any())).thenReturn(paged);

        mockMvc.perform(get("/api/v1/concerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Upcoming concerts fetched successfully"));
    }

    @Test
    void getFeaturedConcerts_success_returns200() throws Exception {
        when(concertService.getFeaturedConcerts()).thenReturn(List.of(mockSummary));

        mockMvc.perform(get("/api/v1/concerts/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Featured concerts are fetched successfully"));
    }

    @Test
    void searchConcerts_success_returns200() throws Exception {
        PagedResponse<ConcertSummaryResponse> paged = PagedResponse.<ConcertSummaryResponse>builder()
                        .content(List.of(mockSummary)).page(0)
                        .size(10).totalElements(1).totalPages(1).first(true).last(true)
                        .build();
        when(concertService.searchConcerts(eq("Coldplay"), any())).thenReturn(paged);

        mockMvc.perform(get("/api/v1/concerts/search").param("query", "Coldplay"))
                .andExpect(status().isOk());
    }

    @Test
    void getConcertDetail_success_returns200() throws Exception {
        UUID concertId = UUID.randomUUID();
        when(concertService.getConcertDetail(concertId)).thenReturn(mockConcertResponse);

        mockMvc.perform(get("/api/v1/concerts/{id}", concertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Coldplay"));
    }

    @Test
    void getConcertDetail_notFound_returns404() throws Exception {
        UUID concertId = UUID.randomUUID();
        when(concertService.getConcertDetail(concertId))
                .thenThrow(new ResourceNotFoundException("Concert not found"));

        mockMvc.perform(get("/api/v1/concerts/{id}", concertId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createConcert_success_returns201() throws Exception {
        ConcertRequest request = new ConcertRequest();
        request.setTitle("Coldplay");
        request.setArtistName("Coldplay");
        request.setDescription("Test concert");
        request.setVenueId(UUID.randomUUID());
        request.setConcertDate(LocalDateTime.now().plusDays(30));
        request.setSaleStartTime(LocalDateTime.now().plusDays(1));
        request.setCategoryId(testCategory.getId());
        when(concertService.createConcert(any(), any())).thenReturn(mockConcertResponse);

        mockMvc.perform(post("/api/v1/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Concert created successfully"));
    }

    @Test
    void createConcert_invalidRequest_returns400() throws Exception {

        mockMvc.perform(post("/api/v1/concerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(concertService);
    }

    @Test
    void addTicketTier_success_returns201() throws Exception {
        UUID concertId = UUID.randomUUID();
        CreateTicketTierRequest request = new CreateTicketTierRequest();
        request.setTierName("VIP");
        request.setSectionId(UUID.randomUUID());
        request.setPrice(BigDecimal.valueOf(1000));
        request.setTotalQuantity(100);
        request.setMaxPerUser(5);
        request.setSectionType(SectionType.GA);

        TicketTierResponse tierResponse = TicketTierResponse.builder()
                .id(UUID.randomUUID()).tierName("VIP").price(BigDecimal.valueOf(1000))
                .tierStatus(TierStatus.UPCOMING)
                .build();

        when(concertService.addTicketTier(eq(concertId), any(), any()))
                .thenReturn(tierResponse);

        mockMvc.perform(post("/api/v1/concerts/{id}/tiers", concertId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Ticket tier added successfully"));
    }

    @Test
    void publishConcert_success_returns200() throws Exception {
        UUID concertId = UUID.randomUUID();
        when(concertService.publishConcert(eq(concertId), any()))
                .thenReturn(mockConcertResponse);

        mockMvc.perform(patch("/api/v1/concerts/{id}/publish", concertId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Concert published successfully"));
    }

    @Test
    void getMyConcerts_success_returns200() throws Exception {
        PagedResponse<ConcertSummaryResponse> paged = PagedResponse.<ConcertSummaryResponse>builder().content(List.of(mockSummary))
                        .page(0).size(10).totalElements(1).totalPages(1).first(true).last(true)
                        .build();
        when(concertService.getOrganizerConcerts(any(), any())).thenReturn(paged);

        mockMvc.perform(get("/api/v1/concerts/my-concerts"))
                .andExpect(status().isOk());
    }

    @Test
    void getNearbyEvents_success_returns200() throws Exception {
        PagedResponse<ConcertSummaryResponse> paged = PagedResponse.<ConcertSummaryResponse>builder().content(List.of(mockSummary)).page(0)
                        .size(10).totalElements(1).totalPages(1).first(true).last(true)
                        .build();
        when(concertService.getNearbyEvents(any(Double.class), any(Double.class), any(), any())).thenReturn(paged);

        mockMvc.perform(get("/api/v1/concerts/nearby")
                        .param("lat", "23.2599")
                        .param("lng", "77.4126"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Nearby events fetched successfully"));
    }

    @Test
    void getByCategory_success_returns200() throws Exception {
        PagedResponse<ConcertSummaryResponse> paged = PagedResponse.<ConcertSummaryResponse>builder()
                        .content(List.of(mockSummary)).page(0).size(10)
                        .totalElements(1).totalPages(1).first(true).last(true)
                        .build();
        UUID categoryId = UUID.randomUUID();
        when(concertService.getByCategory(eq(categoryId), any())).thenReturn(paged);
        mockMvc.perform(get("/api/v1/concerts/category/{categoryId}",categoryId));


    }
}
