package com.concertbooking.concert_booking.unit.concert;
import com.concertbooking.concert_booking.common.enums.*;
import com.concertbooking.concert_booking.common.exception.AuthException;
import com.concertbooking.concert_booking.common.exception.BookingException;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.concert.dto.*;
import com.concertbooking.concert_booking.concert.entity.Concert;
import com.concertbooking.concert_booking.concert.entity.EventCategory;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.mapper.ConcertMapper;
import com.concertbooking.concert_booking.concert.repository.ConcertRepository;
import com.concertbooking.concert_booking.concert.repository.EventCategoryRepository;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.concert.service.ConcertService;
import com.concertbooking.concert_booking.review.repository.OrganizerReviewRepository;
import com.concertbooking.concert_booking.seat.repository.SeatInventoryRepository;
import com.concertbooking.concert_booking.user.entity.User;
import com.concertbooking.concert_booking.venue.dto.VenueResponse;
import com.concertbooking.concert_booking.venue.entity.Venue.Venue;
import com.concertbooking.concert_booking.venue.mapper.VenueMapper;
import com.concertbooking.concert_booking.venue.repository.VenueRepository;
import com.concertbooking.concert_booking.venue.repository.VenueSectionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class ConcertServiceTest {
    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private TicketTierRepository ticketTierRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenueSectionRepository venueSectionRepository;

    @Mock
    private ConcertMapper concertMapper;

    @Mock
    private VenueMapper venueMapper;
    @Mock
    private EventCategoryRepository eventCategoryRepository;
    @Mock
    private SeatInventoryRepository seatInventoryRepository;
    @Mock
    private OrganizerReviewRepository organizerReviewRepository;
    @InjectMocks
    private ConcertService concertService;

    private UUID concertId;
    private UUID venueId;
    private EventCategory testCategory;
    private UUID categoryId = UUID.randomUUID();
    private Concert concert;
    private Venue venue;
    private User organizer;

    private ConcertSummaryResponse summaryResponse;

    @BeforeEach
    void setUp() {

        concertId = UUID.randomUUID();
        venueId = UUID.randomUUID();

        organizer = User.builder()
                .id(UUID.randomUUID())
                .email("org@test.com")
                .build();

        venue = Venue.builder()
                .id(venueId)
                .name("Test Venue")
                .city("Bhopal")
                .isActive(true)
                .build();
        testCategory = EventCategory.builder()
                .id(categoryId)
                .name("Live Music")
                .active(true)
                .build();
        concert = Concert.builder()
                .id(concertId)
                .title("Coldplay")
                .artistName("Coldplay")
                .venue(venue)
                .organizer(organizer)
                .status(ConcertStatus.PUBLISHED)
                .concertDate(LocalDateTime.now().plusDays(10))
                .build();

        summaryResponse =
                new ConcertSummaryResponse(
                        concertId,
                        "Coldplay",
                        "Coldplay",
                        null,
                        "Test Venue",
                        "Bhopal",
                        LocalDateTime.now().plusDays(10),
                        LocalDateTime.now(),
                        ConcertStatus.PUBLISHED,
                        false,
                        false,
                        BigDecimal.valueOf(1000),
                        "Live Music",
                        null,
                        null,
                        null
                );
    }
    @Test
    void getUpcomingConcerts_shouldReturnPagedResponse() {

        Page<ConcertSummaryResponse> page =
                new PageImpl<>(List.of(summaryResponse));

        when(
                concertRepository.findUpcomingConcerts(
                        any(LocalDateTime.class),
                        any()
                )
        ).thenReturn(page);

        var result =
                concertService.getUpcomingConcerts(
                        PageRequest.of(0, 10)
                );

        assertEquals(1, result.getContent().size());

        verify(concertRepository)
                .findUpcomingConcerts(
                        any(LocalDateTime.class),
                        any()
                );
    }
    @Test
    void getConcertByCity_shouldReturnPagedResponse() {

        Page<ConcertSummaryResponse> page =
                new PageImpl<>(List.of(summaryResponse));

        when(
                concertRepository.findByCityProjection(
                        eq("Delhi"),
                        eq(ConcertStatus.PUBLISHED),
                        any()
                )
        ).thenReturn(page);

        var result =
                concertService.getConcertByCity(
                        "Delhi",
                        PageRequest.of(0, 10)
                );

        assertEquals(1, result.getContent().size());

        verify(concertRepository)
                .findByCityProjection(
                        eq("Delhi"),
                        eq(ConcertStatus.PUBLISHED),
                        any()
                );
    }
    @Test
    void searchConcerts_shouldReturnPagedResponse() {

        Page<ConcertSummaryResponse> page = new PageImpl<>(List.of(summaryResponse));
        when(concertRepository.findBySearchProjection(eq("coldplay"),
                        eq(ConcertStatus.PUBLISHED),any())
        ).thenReturn(page);

        var result=concertService.searchConcerts("coldplay",
                        PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        verify(concertRepository).findBySearchProjection(
                        eq("coldplay"),
                        eq(ConcertStatus.PUBLISHED),
                        any()
                );
    }
    @Test
    void getFeaturedConcerts_shouldReturnList() {
        when(concertRepository.findFeaturedProjection(eq(ConcertStatus.PUBLISHED),
                        any(LocalDateTime.class))).thenReturn(List.of(summaryResponse));

        List<ConcertSummaryResponse> result = concertService.getFeaturedConcerts();

        assertEquals(1, result.size());

        verify(concertRepository).findFeaturedProjection(
                        eq(ConcertStatus.PUBLISHED),
                        any(LocalDateTime.class)
                );
    }
    @Test
    void getByCategory_shouldReturnPagedResponse() {
        Page<ConcertSummaryResponse> page=new PageImpl<>(List.of(summaryResponse));
        when(concertRepository.findByCategoryProjection(
                        eq(categoryId),
                        any(LocalDateTime.class),
                        any()
                )
        ).thenReturn(page);

        var result = concertService.getByCategory(
                        categoryId,
                        PageRequest.of(0, 10)
                );

        assertEquals(1, result.getContent().size());

        verify(concertRepository)
                .findByCategoryProjection(
                        eq(categoryId),
                        any(LocalDateTime.class),
                        any()
                );
    }
    @Test
    void getOrganizerConcerts_shouldReturnPagedResponse() {

        Page<ConcertSummaryResponse> page =
                new PageImpl<>(List.of(summaryResponse));

        when(
                concertRepository.findOrganizerConcertsProjection(
                        eq(organizer.getId()),
                        any()
                )
        ).thenReturn(page);

        var result =
                concertService.getOrganizerConcerts(
                        organizer.getId(),
                        PageRequest.of(0, 10)
                );

        assertEquals(1, result.getContent().size());

        verify(concertRepository)
                .findOrganizerConcertsProjection(
                        eq(organizer.getId()),
                        any()
                );
    }
    @Test
    void getConcertDetail_shouldReturnConcert() {

        when(concertRepository.findByIdWithVenue(concertId))
                .thenReturn(Optional.of(concert));

        when(ticketTierRepository.findByConcertIdAndIsActiveTrue(concertId))
                .thenReturn(List.of());

        when(venueMapper.toResponse(venue))
                .thenReturn(
                        VenueResponse.builder()
                                .id(venueId)
                                .name("Test Venue")
                                .city("Bhopal")
                                .build()
                );

        ConcertResponse result =
                concertService.getConcertDetail(concertId);

        assertEquals("Coldplay", result.getTitle());
    }
    @Test
    void getConcertDetail_shouldThrowWhenNotFound() {

        when(concertRepository.findByIdWithVenue(concertId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> concertService.getConcertDetail(concertId)
        );
    }
    @Test
    void getConcertEntityById_shouldReturnConcert() {

        when(concertRepository.findByIdWithVenue(concertId))
                .thenReturn(Optional.of(concert));

        Concert result =
                concertService.getConcertEntityById(concertId);

        assertEquals(concertId, result.getId());
    }
    @Test
    void getConcertEntityById_shouldThrowWhenNotFound() {

        when(concertRepository.findByIdWithVenue(concertId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> concertService.getConcertEntityById(concertId)
        );
    }
    @Test
    void createConcert_shouldCreateConcert() {

        ConcertRequest request = new ConcertRequest();

        request.setTitle("Coldplay");
        request.setArtistName("Coldplay");
        request.setDescription("Music");
        request.setVenueId(venueId);
        request.setCategoryId(categoryId);
        when(eventCategoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        request.setConcertDate(LocalDateTime.now().plusDays(10));
        request.setSaleStartTime(LocalDateTime.now());

        when(venueRepository.findById(venueId))
                .thenReturn(Optional.of(venue));

        when(concertRepository.save(any(Concert.class)))
                .thenReturn(concert);

        when(ticketTierRepository.findByConcertIdAndIsActiveTrue(concertId))
                .thenReturn(List.of());

        when(venueMapper.toResponse(any()))
                .thenReturn(
                        VenueResponse.builder()
                                .id(venueId)
                                .name("Test Venue")
                                .build()
                );

        ConcertResponse response =
                concertService.createConcert(request, organizer);

        assertEquals("Coldplay", response.getTitle());

        verify(concertRepository)
                .save(any(Concert.class));
    }
    @Test
    void createConcert_shouldThrowWhenVenueNotFound() {

        ConcertRequest request = new ConcertRequest();

        request.setVenueId(venueId);

        when(venueRepository.findById(venueId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> concertService.createConcert(request, organizer)
        );
    }
    @Test
    void publishConcert_shouldPublishConcert() {

        TicketTier tier = TicketTier.builder()
                .id(UUID.randomUUID())
                .tierStatus(TierStatus.UPCOMING)
                .isActive(true)
                .build();

        when(concertRepository.findByIdWithVenue(concertId))
                .thenReturn(Optional.of(concert));

        when(ticketTierRepository.findByConcertIdAndIsActiveTrue(concertId)
        ).thenReturn(List.of(tier));

        when(concertRepository.save(any(Concert.class)))
                .thenReturn(concert);


        when(venueMapper.toResponse(any()))
                .thenReturn(
                        VenueResponse.builder()
                                .id(venueId)
                                .name("Test Venue")
                                .build()
                );

        ConcertResponse response = concertService.publishConcert(concertId, organizer);

        assertEquals(ConcertStatus.PUBLISHED, concert.getStatus());

        assertEquals(TierStatus.ACTIVE, tier.getTierStatus());

        verify(ticketTierRepository).saveAll(anyList());
    }
    @Test
    void publishConcert_shouldThrowWhenNotOrganizer() {

        User anotherUser = User.builder()
                .id(UUID.randomUUID())
                .email("another@test.com")
                .build();

        when(concertRepository.findByIdWithVenue(concertId))
                .thenReturn(Optional.of(concert));

        assertThrows(
                AuthException.class,
                () -> concertService.publishConcert(
                        concertId,
                        anotherUser
                )
        );
    }
    @Test
    void publishConcert_shouldThrowWhenNoTicketTiers() {

        when(concertRepository.findByIdWithVenue(concertId))
                .thenReturn(Optional.of(concert));

        when(ticketTierRepository.findByConcertIdAndIsActiveTrue(concertId)
        ).thenReturn(List.of());

        assertThrows(BookingException.class,
                () -> concertService.publishConcert(
                        concertId,
                        organizer
                )
        );
    }
    @Test
    void publishConcert_shouldThrowWhenConcertNotFound() {

        when(concertRepository.findByIdWithVenue(concertId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> concertService.publishConcert(
                        concertId,
                        organizer
                )
        );
    }
}
