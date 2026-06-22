package com.concertbooking.concert_booking.venue.service;


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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueService {
    private final VenueRepository venueRepository;
    private final VenueSectionRepository venueSectionRepository;
    private final VenueMapper venueMapper;
    @Cacheable(value="venues",key="'all'")
    public List<VenueResponse> getAllVenues(){
        return venueMapper.toResponseList(venueRepository.findAllActive());

    }

    @Cacheable(value = "venues",key="#city.toLowerCase()")
    public List<VenueResponse> getVenuesByCity(String city){
        return venueMapper.toResponseList(
                venueRepository.findByCityIgnoreCaseAndIsActiveTrue(city));
    }
    @Cacheable(value="venue",key="#venueId")
    public  VenueResponse getVenueById(UUID venueId){
        return venueMapper.toResponse(getVenueEntity(venueId));

    }

    @Transactional
    @CacheEvict(value="venues",allEntries = true)
    public VenueResponse createVenue(VenueRequest request) {
        if (venueRepository.existsActiveVenueByName(request.getName())) {
            throw new ResourceNotFoundException("Venue with this name already exists");
        }
        Venue venue = Venue.builder()
                .name(request.getName())
                .city(request.getCity())
                .address(request.getAddress())
                .venueType(request.getVenueType())
                .totalCapacity(request.getTotalCapacity())
                .googleMapsURL(request.getGoogleMapsURL())
                .locationDescription(request.getLocationDescription())
                .layoutImageUrl(request.getLayoutImageUrl())
                .isActive(true)
                .build();
        extractCoordinates(venue, request.getGoogleMapsURL());
        log.info("Creating venue: {}", venue.getName());
        return venueMapper.toResponse(venueRepository.save(venue));
    }
        @Transactional
        @CacheEvict(value = {"venues", "venue"}, allEntries = true)
        public VenueResponse updateVenue (UUID venueId, VenueRequest request){
            Venue existing = getVenueEntity(venueId);
            existing.setName(request.getName());
            existing.setCity(request.getCity());
            existing.setAddress(request.getAddress());
            existing.setTotalCapacity(request.getTotalCapacity());
            existing.setGoogleMapsURL(request.getGoogleMapsURL());
            extractCoordinates(existing, request.getGoogleMapsURL());
            existing.setLocationDescription(request.getLocationDescription());
            existing.setLayoutImageUrl(request.getLayoutImageUrl());
            log.info("Updated venue: {}", venueId);
            return venueMapper.toResponse(venueRepository.save(existing));
        }

        @Transactional
        @CacheEvict(value = {"venues", "venue"}, allEntries = true)
        public void deactivateVenue (UUID venueId){
            Venue venue = getVenueEntity(venueId);
            venue.setActive(false);
            venueRepository.save(venue);
            log.info("Deactivated Venue: {}", venueId);
        }
        //Sections
        public List<SectionResponse> getSectionsByVenue (UUID venueId){
            getVenueEntity(venueId);
            return venueMapper.toSectionResponseList(
                    venueSectionRepository.findByVenueIdAndIsActiveTrue(venueId));
        }
        @Transactional
        @CacheEvict(value = {"venues", "venue"}, allEntries = true)
        public SectionResponse addSection(UUID venueId, SectionRequest request) {
            Venue venue = getVenueEntity(venueId);
            VenueSection section = VenueSection.builder()
                    .venue(venue)
                    .name(request.getName())
                    .sectionType(request.getSectionType())
                    .totalCapacity(request.getTotalCapacity())
                    .xPosition(request.getXPosition())
                    .yPosition(request.getYPosition())
                    .width(request.getWidth())
                    .height(request.getHeight())
                    .colorHex(request.getColorHex())
                    .isActive(true)
                    .build();
            log.info("Adding section {} to venue {}", section.getName(), venueId);
            return venueMapper.toSectionResponse(venueSectionRepository.save(section));
        }
    @Transactional
    @CacheEvict(value={"venues","venue"},allEntries = true)
    public void deactivateSection(UUID sectionId) {
        VenueSection section = venueSectionRepository.findById(sectionId).orElseThrow(() -> new
                ResourceNotFoundException("Section Not Found" + sectionId));
        section.setActive(false);
        venueSectionRepository.save(section);
        log.info("Deactivated section: {}", sectionId);
    }

    public Venue getVenueEntity(UUID venueId) {
            return venueRepository.findById(venueId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Venue not found: " + venueId));
        }

    private void extractCoordinates(Venue venue, String url) {
        if (url == null||url.isBlank()){
            return;
        }
        Pattern pattern = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            venue.setLatitude(Double.parseDouble(matcher.group(1)));
            venue.setLongitude(Double.parseDouble(matcher.group(2)));
        }
    }
    }

//just a comment to check if the pipeline is working well or not

