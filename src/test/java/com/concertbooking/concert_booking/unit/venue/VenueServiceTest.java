package com.concertbooking.concert_booking.unit.venue;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.venue.dto.SectionRequest;
import com.concertbooking.concert_booking.venue.dto.SectionResponse;
import com.concertbooking.concert_booking.venue.dto.VenueRequest;
import com.concertbooking.concert_booking.venue.dto.VenueResponse;
import com.concertbooking.concert_booking.venue.entity.Venue.Venue;
import com.concertbooking.concert_booking.venue.entity.Venue.VenueSection;
import com.concertbooking.concert_booking.venue.mapper.VenueMapper;
import com.concertbooking.concert_booking.venue.repository.VenueRepository;
import com.concertbooking.concert_booking.venue.repository.VenueSectionRepository;
import com.concertbooking.concert_booking.venue.service.VenueService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenueSectionRepository venueSectionRepository;

    @Mock
    private VenueMapper venueMapper;

    @InjectMocks
    private VenueService venueService;

    private UUID venueId;
    private UUID sectionId;

    private Venue venue;
    private VenueResponse venueResponse;

    private VenueSection section;
    private SectionResponse sectionResponse;

    @BeforeEach
    void setUp() {

        venueId = UUID.randomUUID();
        sectionId = UUID.randomUUID();

        venue = Venue.builder()
                .id(venueId)
                .name("Test Venue")
                .city("Bhopal")
                .address("MP")
                .totalCapacity(1000)
                .isActive(true)
                .build();

        venueResponse = VenueResponse.builder()
                .id(venueId)
                .name("Test Venue")
                .city("Bhopal")
                .build();

        section = VenueSection.builder()
                .id(sectionId)
                .venue(venue)
                .name("VIP")
                .totalCapacity(100)
                .isActive(true)
                .build();

        sectionResponse = SectionResponse.builder()
                .id(sectionId)
                .name("VIP")
                .build();
    }

    @Test
    void getAllVenues_shouldReturnMappedVenues() {

        when(venueRepository.findAllActive()).thenReturn(List.of(venue));

        when(venueMapper.toResponseList(List.of(venue))).thenReturn(List.of(venueResponse));

        List<VenueResponse> result = venueService.getAllVenues();

        assertEquals(1, result.size());

        verify(venueRepository).findAllActive();
    }
    @Test
    void getVenuesByCity_shouldReturnMappedVenues() {

        when(venueRepository.findByCityIgnoreCaseAndIsActiveTrue("Bhopal")
        ).thenReturn(List.of(venue));

        when(venueMapper.toResponseList(List.of(venue))).thenReturn(List.of(venueResponse));

        List<VenueResponse> result = venueService.getVenuesByCity("Bhopal");

        assertEquals(1, result.size());
    }
    @Test
    void getVenueById_shouldReturnVenue() {

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        when(venueMapper.toResponse(venue)).thenReturn(venueResponse);

        VenueResponse result = venueService.getVenueById(venueId);

        assertEquals(venueId, result.getId());
    }

    @Test
    void getVenueById_shouldThrowWhenNotFound() {

        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> venueService.getVenueById(venueId)
        );
    }
    @Test
    void createVenue_shouldCreateVenue() {

        VenueRequest request = new VenueRequest();

        request.setName("Test Venue");
        request.setCity("Bhopal");
        request.setAddress("MP");
        request.setTotalCapacity(1000);

        when(venueRepository.save(any(Venue.class))).thenReturn(venue);

        when(venueMapper.toResponse(venue)).thenReturn(venueResponse);

        VenueResponse result = venueService.createVenue(request);

        assertEquals("Test Venue", result.getName());

        verify(venueRepository).save(any(Venue.class));
    }
    @Test
    void updateVenue_shouldUpdateVenue() {

        VenueRequest request = new VenueRequest();
        request.setName("Updated Venue");
        request.setCity("Indore");
        request.setAddress("MP");
        request.setTotalCapacity(2000);

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        when(venueRepository.save(any(Venue.class))).thenReturn(venue);

        when(venueMapper.toResponse(any(Venue.class))).thenReturn(venueResponse);

        VenueResponse result = venueService.updateVenue(venueId, request);

        assertEquals(venueResponse.getId(), result.getId());

        verify(venueRepository).save(any(Venue.class));
    }
    @Test
    void updateVenue_shouldThrowWhenVenueNotFound() {

        VenueRequest request = new VenueRequest();

        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> venueService.updateVenue(venueId, request)
        );
    }
    @Test
    void deactivateVenue_shouldDeactivateVenue() {

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        venueService.deactivateVenue(venueId);

        verify(venueRepository).save(venue);

        assertFalse(venue.isActive());
    }
    @Test
    void deactivateVenue_shouldThrowWhenVenueNotFound() {

        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> venueService.deactivateVenue(venueId)
        );
    }
    @Test
    void getSectionsByVenue_shouldReturnSections() {

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        when(venueSectionRepository.findByVenueIdAndIsActiveTrue(venueId)
        ).thenReturn(List.of(section));

        when(venueMapper.toSectionResponseList(List.of(section))
        ).thenReturn(List.of(sectionResponse));

        List<SectionResponse> result = venueService.getSectionsByVenue(venueId);

        assertEquals(1, result.size());
    }
    @Test
    void getSectionsByVenue_shouldThrowWhenVenueNotFound() {

        when(venueRepository.findById(venueId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> venueService.getSectionsByVenue(venueId));
    }
    @Test
    void addSection_shouldAddSection() {

        SectionRequest request = new SectionRequest();

        request.setName("VIP");
        request.setTotalCapacity(100);

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        when(venueSectionRepository.save(any(VenueSection.class))
        ).thenReturn(section);

        when(venueMapper.toSectionResponse(section)
        ).thenReturn(sectionResponse);

        SectionResponse result = venueService.addSection(venueId, request);

        assertEquals(sectionResponse.getId(), result.getId());
    }
    @Test
    void deactivateSection_shouldDeactivateSection() {

        when(venueSectionRepository.findById(sectionId)).thenReturn(Optional.of(section));

        venueService.deactivateSection(sectionId);

        verify(venueSectionRepository).save(section);
        assertFalse(section.isActive());
    }
    @Test
    void deactivateSection_shouldThrowWhenSectionNotFound() {

        when(venueSectionRepository.findById(sectionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> venueService.deactivateSection(sectionId));
    }
    @Test
    void getVenueEntity_shouldReturnVenue() {

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        Venue result = venueService.getVenueEntity(venueId);

        assertEquals(venueId, result.getId());
    }
    @Test
    void getVenueEntity_shouldThrowWhenNotFound() {

        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> venueService.getVenueEntity(venueId));
    }
}
