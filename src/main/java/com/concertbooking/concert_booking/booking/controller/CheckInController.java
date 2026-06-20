package com.concertbooking.concert_booking.booking.controller;

import com.concertbooking.concert_booking.booking.service.CheckInService;
import com.concertbooking.concert_booking.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkin")
@RequiredArgsConstructor
@Tag(name = "Check-In", description = "Venue staff QR scan endpoint")
public class CheckInController {
    private final CheckInService checkInService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    @Operation(summary = "Scan QR token and mark ticket as checked in")
    public ResponseEntity<ApiResponse<String>>checkIn(@RequestParam String qrToken) {

        String result = checkInService.checkIn(qrToken);
        return ResponseEntity.ok(ApiResponse.success(result,"Check-in successful"));
    }
}
