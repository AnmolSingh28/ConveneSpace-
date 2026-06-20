package com.concertbooking.concert_booking.queue.controller;

import com.concertbooking.concert_booking.common.exception.ResourceNotFoundException;
import com.concertbooking.concert_booking.common.response.ApiResponse;
import com.concertbooking.concert_booking.concert.entity.TicketTier;
import com.concertbooking.concert_booking.concert.repository.TicketTierRepository;
import com.concertbooking.concert_booking.queue.dto.QueueJoinResponse;
import com.concertbooking.concert_booking.queue.dto.QueuePositionResponse;
import com.concertbooking.concert_booking.queue.dto.QueueStatusResponse;
import com.concertbooking.concert_booking.queue.service.VirtualQueueService;
import com.concertbooking.concert_booking.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
@Tag(name="Virtual Queue",description="High Traffic Queue Management")
public class VirtualQueueController{
    private final VirtualQueueService virtualQueueService;
    private final TicketTierRepository ticketTierRepository;

    @PostMapping("/join/{tierId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Join virtual queue")
    public ResponseEntity<ApiResponse<QueueJoinResponse>> joinQueue(@PathVariable UUID tierId, @AuthenticationPrincipal User user){
        QueueJoinResponse response=virtualQueueService.joinQueue(tierId,user);
        return ResponseEntity.ok(ApiResponse.success(response,"Successfully joined queue")
        );
    }
    @GetMapping("/position/{tierId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get queue position")
    public ResponseEntity<ApiResponse<QueuePositionResponse>> getPosition(@PathVariable UUID tierId,@AuthenticationPrincipal User user){
        QueuePositionResponse response=virtualQueueService.getPosition(tierId,user);
        return ResponseEntity.ok(ApiResponse.success(response,"Queue position fetched")
        );
    }

    @DeleteMapping("/leave/{tierId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Leave queue")
    public ResponseEntity<ApiResponse<Void>> leaveQueue(@PathVariable UUID tierId, @AuthenticationPrincipal User user){
        virtualQueueService.leaveQueue(tierId,user);
        return ResponseEntity.ok(ApiResponse.success(null,"Left queue successfully")
        );
    }

    @GetMapping("/status/{tierId}")
    @Operation(summary = "Get queue status")
    public ResponseEntity<ApiResponse<QueueStatusResponse>> getQueueStatus(@PathVariable UUID tierId){
        QueueStatusResponse response=new QueueStatusResponse(tierId,virtualQueueService.getQueueSize(tierId),
                        virtualQueueService.getRealAvailableQuantity(tierId),
                        virtualQueueService.isQueueActive(tierId)
                );
        return ResponseEntity.ok(ApiResponse.success(response,"Queue status fetched")
        );
    }
    @GetMapping("/concert-id/{tierId}")
    @Operation(summary = "Check queue token",description = "Returns whether the current user has a valid queue token for the tier")
    public ResponseEntity<ApiResponse<UUID>> getConcertIdByTier(@PathVariable UUID tierId){
        TicketTier tier = ticketTierRepository.findById(tierId).orElseThrow(() -> new ResourceNotFoundException("Tier not found"));
        return ResponseEntity.ok(ApiResponse.success(tier.getConcert().getId(), "Success"));
    }
    @GetMapping("/has-token/{tierId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get concert ID from tier")
    public ResponseEntity<ApiResponse<Boolean>> hasToken(
            @PathVariable UUID tierId,
            @AuthenticationPrincipal User user){
        boolean has = virtualQueueService.hasValidQueueToken(tierId, user);
        return ResponseEntity.ok(ApiResponse.success(has, "Success"));
    }
}
