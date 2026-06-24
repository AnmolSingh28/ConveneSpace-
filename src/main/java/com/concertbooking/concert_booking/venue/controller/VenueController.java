package com.concertbooking.concert_booking.venue.controller;

import com.concertbooking.concert_booking.common.response.ApiResponse;
import com.concertbooking.concert_booking.venue.dto.SectionRequest;
import com.concertbooking.concert_booking.venue.dto.SectionResponse;
import com.concertbooking.concert_booking.venue.dto.VenueRequest;
import com.concertbooking.concert_booking.venue.dto.VenueResponse;
import com.concertbooking.concert_booking.venue.entity.Venue.Venue;
import com.concertbooking.concert_booking.venue.entity.Venue.VenueSection;
import com.concertbooking.concert_booking.venue.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
@Tag(name="Venues",description="Venue management")
public class VenueController {
    private final VenueService venueService;

    @GetMapping
    @Operation(summary = "Get all the active venues")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getAllVenues(){
        return ResponseEntity.ok(ApiResponse.success(venueService.getAllVenues(),
                "All the venues are fetched"));
    }
    @GetMapping("/{venueId}")
    @Operation(summary = "Get venue by id")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueById(@PathVariable UUID venueId){
        return ResponseEntity.ok(ApiResponse.success(venueService.getVenueById(venueId)
        ,"Venue for the given id fetched"));
    }
    @GetMapping("/city/{city}")
    @Operation(summary = "Get all the active venues")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getVenuesByCity(@PathVariable String city){
        return ResponseEntity.ok(ApiResponse.success(venueService.getVenuesByCity(city),"" +
                        "Venues for the given city fetched"
                ));
    }
    @GetMapping("/{venueId}/sections")
    @Operation(summary = "Get all sections for a venue")
    public ResponseEntity<ApiResponse<List<SectionResponse>>> getSections(@PathVariable UUID venueId){
        return ResponseEntity.ok(ApiResponse.success(venueService.getSectionsByVenue(venueId)
        ,"Sections are fetched for the given venue ID"));
    }

    //ADMIN ONLY

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary="Create a new venue ")
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(@RequestBody VenueRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(venueService.createVenue(request),"Venue Created Successfully"));
    }
    @PutMapping("/{venueId}") // Update venue
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary="Update an existing venue ")
    public ResponseEntity<ApiResponse<VenueResponse>> updateVenue(@PathVariable UUID venueId ,@RequestBody VenueRequest venue){
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(venueService.updateVenue(venueId,venue),"Venue Updated Successfully"));
    }
    @PostMapping("/{venueId}/sections")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @Operation(summary="Add a section to Venue ")
    public ResponseEntity<ApiResponse<SectionResponse>> addSection(@PathVariable UUID venueId,@RequestBody SectionRequest section){
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(venueService.addSection(venueId,section),"Section Added Successfully"));
    }
    @DeleteMapping("/{venueId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary="Deactivate venue ")
    public ResponseEntity<ApiResponse<Void>> deactivateVenue(@PathVariable UUID venueId){
       venueService.deactivateVenue(venueId);
       return ResponseEntity.ok(ApiResponse.success(null,"Venue Deactivated Successfully"));
    }
    @DeleteMapping("/sections/{sectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary="Deactivate section ")
    public ResponseEntity<ApiResponse<Void>> deactivateSection(@PathVariable UUID sectionId){
       venueService.deactivateSection(sectionId);
       return ResponseEntity.ok(ApiResponse.success(null,"Section Deactivated Successfully"));
    }











}
