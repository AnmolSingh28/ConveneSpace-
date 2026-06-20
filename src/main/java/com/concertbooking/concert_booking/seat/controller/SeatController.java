package com.concertbooking.concert_booking.seat.controller;

import com.concertbooking.concert_booking.common.response.ApiResponse;
import com.concertbooking.concert_booking.seat.dto.GaLockRequest;
import com.concertbooking.concert_booking.seat.dto.SeatLockRequest;
import com.concertbooking.concert_booking.seat.dto.SeatResponse;
import com.concertbooking.concert_booking.seat.service.SeatLockService;
import com.concertbooking.concert_booking.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
@Tag(name = "Seats", description = "Seat Availability and Locking")
public class SeatController {

    private final SeatLockService seatLockService;

    // PUBLIC
    @GetMapping("/tier/{tierId}")
    @Operation(summary = "Get all seats for a tier")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeatsByTier(
            @PathVariable UUID tierId) {
        return ResponseEntity.ok(
                ApiResponse.success(seatLockService.getSeatsByTier(tierId), "Seats are fetched according to given tier"));
    }

    @GetMapping("/tier/{tierId}/available")
    @Operation(summary = "Get available seats for a tier")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getAvailableSeats(
            @PathVariable UUID tierId) {
        return ResponseEntity.ok(
                ApiResponse.success(seatLockService.getAvailableSeats(tierId), "Available seats are fetched"));
    }
    //GA
    @PostMapping("/ga/{tierId}/lock")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lock GA tickets")
    public ResponseEntity<ApiResponse<Void>> lockGa(
            @PathVariable UUID tierId,
            @Valid @RequestBody GaLockRequest request,
            @AuthenticationPrincipal User user) {
        seatLockService.lockGaTier(tierId, request.getQuantity(), user);
        return ResponseEntity.ok( ApiResponse.success(null, "GA tickets locked"));
    }

    @PostMapping("/ga/{tierId}/release")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Release GA tickets")
    public ResponseEntity<ApiResponse<Void>> releaseGa(
            @PathVariable UUID tierId,
            @Valid @RequestBody GaLockRequest request,
            @AuthenticationPrincipal User user){
        seatLockService.releaseGaLock(tierId,  user);
        return ResponseEntity.ok(
                ApiResponse.success(null, "GA tickets released"));
    }

    //ASSIGNED
    @PostMapping("/assigned/lock")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lock assigned seat")
    public ResponseEntity<ApiResponse<Void>> lockSeat(
            @Valid @RequestBody SeatLockRequest request,
            @AuthenticationPrincipal User user){
        seatLockService.lockAssignedSeat(request.getSeatId(), user);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Seat locked"));
    }

    @PostMapping("/assigned/release")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Release assigned seat")
    public ResponseEntity<ApiResponse<Void>> releaseSeat(
            @Valid @RequestBody SeatLockRequest request,
            @AuthenticationPrincipal User user){
        seatLockService.releaseAssignedSeat(request.getSeatId());
        return ResponseEntity.ok(
                ApiResponse.success(null, "Seat released"));
    }

    @PostMapping("/concert/{concertId}/viewing")
    @Operation(summary = "Live viewer count for a concert")
    public ResponseEntity<ApiResponse<Void>> trackViewing(
            @PathVariable UUID concertId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId){
        seatLockService.trackViewing(concertId,sessionId);
        return ResponseEntity.ok(ApiResponse.success(null,"Success in tracking"));

    }
    @PostMapping("/presence/{seatId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> updatePresence(
            @PathVariable UUID seatId,
            @AuthenticationPrincipal User user){
        seatLockService.updateSeatPresence(seatId, user);
        return ResponseEntity.ok(ApiResponse.success(null,"Presence updated")
        );
    }
}