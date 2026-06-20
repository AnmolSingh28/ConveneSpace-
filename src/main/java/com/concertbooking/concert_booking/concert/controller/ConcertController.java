package com.concertbooking.concert_booking.concert.controller;

import com.concertbooking.concert_booking.common.enums.EventType;
import com.concertbooking.concert_booking.common.response.ApiResponse;
import com.concertbooking.concert_booking.common.response.PagedResponse;
import com.concertbooking.concert_booking.concert.dto.*;
import com.concertbooking.concert_booking.concert.service.ConcertService;
import com.concertbooking.concert_booking.concert.service.PreRegistrationService;
import com.concertbooking.concert_booking.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/concerts")
@RequiredArgsConstructor
@Tag(name="Concerts",description = "Concert discovery and management")
public class ConcertController {
    private final ConcertService concertService;
    private final PreRegistrationService preRegistrationService;

    @GetMapping
    @Operation(summary = "Get upcoming concerts")
    public ResponseEntity<ApiResponse<PagedResponse<ConcertSummaryResponse>>>getUpcomingConcerts(
            @PageableDefault(size=10,sort ="concertDate")
            Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success(concertService.getUpcomingConcerts(pageable),"Upcoming concerts fetched successfully"));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured concerts for home")
    public ResponseEntity<ApiResponse<List<ConcertSummaryResponse>>>getFeaturedConcerts(){
        return ResponseEntity.ok(ApiResponse.success(concertService.getFeaturedConcerts(),
                "Featured concerts are fetched successfully"));
    }
    @GetMapping("/search")
    @Operation(summary = "Search concerts by artist or title")
    public ResponseEntity<ApiResponse<PagedResponse<ConcertSummaryResponse>>>searchConcerts(
            @RequestParam String query,
            @PageableDefault(size =10) Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success(concertService.searchConcerts(query,pageable),
                "Provided searching capability"));
    }
    @GetMapping("/city/{city}")
    @Operation(summary = "Get concerts by city")
    public ResponseEntity<ApiResponse<PagedResponse<ConcertSummaryResponse>>>getConcertsByCity(
            @PathVariable String city,
            @PageableDefault(size = 10) Pageable pageable){
        return ResponseEntity.ok(
                ApiResponse.success(concertService.getConcertByCity(city, pageable),"Concerts successfully fetched for the given city"));
    }
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all concerts - Admin only")
    public ResponseEntity<ApiResponse<PagedResponse<ConcertSummaryResponse>>>getAllConcerts(@PageableDefault(size = 20) Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success(concertService.getAllConcerts(pageable), "All concerts fetched"));
    }
   @GetMapping("/{concertId}")
    @Operation(summary = "Get concert detail")
    public ResponseEntity<ApiResponse<ConcertResponse>>getConcertDetail(@PathVariable UUID concertId){
        return ResponseEntity.ok(
                ApiResponse.success(concertService.getConcertDetail(concertId),""));
    }

    // ORGANIZER ENDPOINTS
    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Create concert (Organizer/Admin only)")
    public ResponseEntity<ApiResponse<ConcertResponse>>createConcert(
            @Valid @RequestBody ConcertRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User organizer){
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                        concertService.createConcert(request, organizer),"Concert created successfully"));
    }

    @PostMapping("/{concertId}/tiers")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    @Operation(summary = "Add ticket tier to concert")
    public ResponseEntity<ApiResponse<TicketTierResponse>>addTicketTier(
            @PathVariable UUID concertId,
            @Valid @RequestBody CreateTicketTierRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User organizer) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        concertService.addTicketTier(concertId, request, organizer),"Ticket tier added successfully"));
    }

    @PatchMapping("/{concertId}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    @Operation(summary = "Publish concert DRAFT to PUBLISHED")
    public ResponseEntity<ApiResponse<ConcertResponse>>publishConcert(
            @PathVariable UUID concertId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User organizer) {
        return ResponseEntity.ok(
                ApiResponse.success(concertService.publishConcert(concertId, organizer), "Concert published successfully"));
    }

    @GetMapping("/my-concerts")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    @Operation(summary = "Get organizer's own concerts")
    public ResponseEntity<ApiResponse<PagedResponse<ConcertSummaryResponse>>>getMyConcerts(
            @Parameter(hidden = true)
            @AuthenticationPrincipal User organizer,
            @PageableDefault(size=10) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(concertService.getOrganizerConcerts(organizer.getId(),pageable),"Fetched concerts successfully"));
    }
    @PostMapping("/{concertId}/pre-register")
    @Operation(summary = "Pre-register for a high demand concert")
    public ResponseEntity<ApiResponse<PreRegistrationResponse>>preRegister(
            @PathVariable UUID concertId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(preRegistrationService.register(concertId, user), "Pre-registration successful"));
    }

    @PatchMapping("/{concertId}/assign-queue")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    @Operation(summary = "Assign randomized queue positions — organizer only")
    public ResponseEntity<ApiResponse<Void>>assignQueue(
            @PathVariable UUID concertId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal User organizer) {
        preRegistrationService.assignQueuePositions(concertId, organizer);
        return ResponseEntity.ok(
                ApiResponse.success(null,"Queue positions assigned successfully"));
    }

    @GetMapping("/{concertId}/my-registration")
    @Operation(summary = "Get my pre-registration status")
    public ResponseEntity<ApiResponse<PreRegistrationResponse>>getMyRegistration(
            @PathVariable UUID concertId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ApiResponse.success(preRegistrationService.getMyRegistration(concertId, user),"Success"));
    }

    @GetMapping("/{concertId}/registration-count")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    @Operation(summary = "Get total pre-registration count — organizer only")
    public ResponseEntity<ApiResponse<Long>>getRegistrationCount(@PathVariable UUID concertId) {
        return ResponseEntity.ok(
                ApiResponse.success(preRegistrationService.getRegistrationCount(concertId),"Success"));
    }
    @GetMapping("/nearby")
    @Operation(summary = "Get events near user location",
            description = "radius: 25, 50, 100 km or omit for anywhere")
    public ResponseEntity<ApiResponse<PagedResponse<ConcertSummaryResponse>>>getNearbyEvents(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required=false) Double radiusKm,
            @PageableDefault(size=10) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(concertService.getNearbyEvents(lat,lng,radiusKm,pageable),"Nearby events fetched successfully"));
    }
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Filter events by category")
    public ResponseEntity<ApiResponse<PagedResponse<ConcertSummaryResponse>>>getByCategory(
            @PathVariable UUID categoryId,
            @PageableDefault(size=10) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(concertService.getByCategory(categoryId,pageable),
                        "Events fetched for category: "+categoryId));
    }
    @GetMapping("/organizer/{organizerId}")
    @Operation(summary = "Get concerts by organizer")
    public ResponseEntity<ApiResponse<PagedResponse<ConcertSummaryResponse>>>getOrganizerConcerts(
            @PathVariable UUID organizerId,
            @PageableDefault(size = 6) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(concertService.getOrganizerConcerts(organizerId,pageable),
                        "Organizer concerts fetched"));
    }

    @PatchMapping("/{concertId}/cancel")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    @Operation(summary = "Cancel a concert")
    public ResponseEntity<ApiResponse<ConcertResponse>>cancelConcert(
            @PathVariable UUID concertId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(concertService.cancelConcert(concertId,user,reason),
                "Concert cancelled"));
    }

    @PatchMapping("/{concertId}/postpone")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    @Operation(summary = "Postpone a concert")
    public ResponseEntity<ApiResponse<ConcertResponse>>postponeConcert(
            @PathVariable UUID concertId,
            @RequestParam LocalDateTime newDate,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(concertService.postponeConcert(concertId,newDate,user),
                "Concert postponed"));
    }

    @PatchMapping("/{concertId}/feature")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Feature/unfeature a concert")
    public ResponseEntity<ApiResponse<ConcertResponse>>featureConcert(
            @PathVariable UUID concertId,
            @RequestParam boolean featured) {
        return ResponseEntity.ok(ApiResponse.success(concertService.setFeatured(concertId,featured),
                featured ? "Concert featured" : "Concert unfeatured"));
    }


}
