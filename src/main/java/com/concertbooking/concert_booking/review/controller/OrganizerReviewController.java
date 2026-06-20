package com.concertbooking.concert_booking.review.controller;

import com.concertbooking.concert_booking.common.response.ApiResponse;
import com.concertbooking.concert_booking.review.dto.CreateOrganizerReviewRequest;
import com.concertbooking.concert_booking.review.dto.OrganizerRatingSummaryResponse;
import com.concertbooking.concert_booking.review.dto.OrganizerReviewResponse;
import com.concertbooking.concert_booking.review.service.OrganizerReviewService;
import com.concertbooking.concert_booking.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name="Organizer Review" , description = "Organizer Review Management")
public class OrganizerReviewController {
    private final OrganizerReviewService organizerReviewService;

    @PostMapping("/{bookingId}")
    @Operation(summary = "Create a review")
    public ApiResponse<String> createReview(@PathVariable UUID bookingId,
            @Valid @RequestBody CreateOrganizerReviewRequest request,
            @AuthenticationPrincipal User currentUser
    ){organizerReviewService.createReview(bookingId,request,currentUser);
        return ApiResponse.success("Review submitted successfully");
    }

    @GetMapping("/organizer/{organizerId}")
    @Operation(summary = "Fetch organizer review")
    public ApiResponse<Page<OrganizerReviewResponse>> getOrganizerReviews(@PathVariable UUID organizerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<OrganizerReviewResponse> reviews=organizerReviewService.getOrganizerReviews(organizerId,
                        PageRequest.of(page, size));
        return ApiResponse.success(reviews);
    }
    @GetMapping("/organizer/{organizerId}/summary")
    @Operation(summary = "Fetch organizer rating summary")
    public ApiResponse<OrganizerRatingSummaryResponse> getOrganizerRatingSummary(@PathVariable UUID organizerId){
        return ApiResponse.success(organizerReviewService.getOrganizerRatingSummary(organizerId));
    }
}
