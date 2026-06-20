package com.concertbooking.concert_booking.booking.controller;

import com.concertbooking.concert_booking.booking.dto.BookingRequest;
import com.concertbooking.concert_booking.booking.dto.BookingResponse;
import com.concertbooking.concert_booking.booking.service.BookingService;
import com.concertbooking.concert_booking.common.enums.BookingStatus;
import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.common.response.ApiResponse;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name="Bookings",description="Bookings happen here")
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @Operation(summary="Creating booking instances")
    public ResponseEntity<ApiResponse<BookingResponse>>createBooking(
            @Valid  @RequestBody BookingRequest request,
            @AuthenticationPrincipal User user){
        return ResponseEntity.status(HttpStatus.CREATED ).body(ApiResponse.success(
                    bookingService.createBooking(request,user),"Booking created successfully"));
    }
    @GetMapping("/my-bookings")
    @Operation(summary = "Get current user bookings")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>>getMyBookings(
            @AuthenticationPrincipal User user,
            @PageableDefault(size=10,sort="createdAt") Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success(
                        bookingService.getUserBookings(user,pageable),"Bookings have been successfully fetched")
        );
    }
    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get booking by reference number")
    public ResponseEntity<ApiResponse<BookingResponse>>getByReference(
            @PathVariable String reference,
            @AuthenticationPrincipal User user){
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingByReference(reference,user),
                        " Booking has been successfully fetched by reference ")
        );
    }
    @GetMapping("{bookingId}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>>getById(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User user){
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.getBookingById(bookingId,user),
                        "Booking has been successfully fetched by booking ID")
        );
    }
    @DeleteMapping("/{bookingId}")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<BookingResponse>>cancelBooking(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal User user
    ){
        return ResponseEntity.ok(
                ApiResponse.success(bookingService.cancelBooking(bookingId,user,reason),
                        "Booking has been cancelled successfully"
                )
        );
    }
    @GetMapping("/estimate")
    @Operation(summary = "Get price estimate before booking")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEstimate(
            @RequestParam UUID tierId,
            @RequestParam int quantity){
        return ResponseEntity.ok(ApiResponse.success(bookingService.getEstimate(tierId,quantity),"Estimate calculated")
        );
    }
    @GetMapping("/can-book")
    @Operation(summary = "Check if user can book for this concert")
    public ResponseEntity<ApiResponse<Map<String, Object>>> canBook(
            @RequestParam UUID tierId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.canBook(tierId,user),"Booking can be done"));

    }

}
