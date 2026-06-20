package com.concertbooking.concert_booking.analytics.controller;

import com.concertbooking.concert_booking.analytics.dto.DashboardMetricsResponse;
import com.concertbooking.concert_booking.analytics.service.OrganizerAnalyticsService;
import com.concertbooking.concert_booking.common.response.ApiResponse;
import com.concertbooking.concert_booking.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name="Organizer Analytics")
public class AnalyticsController {
    private final OrganizerAnalyticsService organizerAnalyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Organizer Analytics Dashboard")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDashboardMetrics(@AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success(
                organizerAnalyticsService.getDashboardMetrics(currentUser.getId()),"Metrics fetched"));

    }
}
