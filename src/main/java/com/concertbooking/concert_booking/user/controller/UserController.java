package com.concertbooking.concert_booking.user.controller;

import com.concertbooking.concert_booking.common.enums.UserRole;
import com.concertbooking.concert_booking.common.response.ApiResponse;
import com.concertbooking.concert_booking.common.response.PagedResponse;
import com.concertbooking.concert_booking.concert.dto.ConcertResponse;
import com.concertbooking.concert_booking.concert.dto.ConcertSummaryResponse;
import com.concertbooking.concert_booking.concert.service.ConcertService;
import com.concertbooking.concert_booking.user.dto.ChangePasswordRequest;
import com.concertbooking.concert_booking.user.dto.UpdateProfileRequest;
import com.concertbooking.concert_booking.user.dto.UserProfileResponse;
import com.concertbooking.concert_booking.user.entity.User;
import com.concertbooking.concert_booking.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name="Users",description = "User profile management")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal User user){
        return ResponseEntity.ok( ApiResponse.success(userService.getProfile(user),"Success"));
    }
    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public  ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request
            ){
        return ResponseEntity.ok( ApiResponse.success(userService.updateProfile(user,request),"Profile update successfully")
        );
    }
    @PatchMapping("/me/change-password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user,request.getCurrentPassword(),request.getNewPassword());
        return ResponseEntity.ok( ApiResponse.success(null,"Password changed successfully"));
    }
    @DeleteMapping("/me")
    @Operation(summary = "Deactivate user account")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@AuthenticationPrincipal User user,@RequestParam String password){
        userService.deactivateAccount(user,password);
        return ResponseEntity.ok(ApiResponse.success(null,"User account deactivated successfully"
        ));
    }
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users  (Admin only)")
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>>getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(pageable),"Users fetched"));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID  (Admin only)")
    public ResponseEntity<ApiResponse<UserProfileResponse>>getUserById(
        @PathVariable UUID userId) {
    return ResponseEntity.ok(
            ApiResponse.success(userService.getUserById(userId),"Success"));
}


    @PatchMapping("/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ban a user  (Admin only)")
    public ResponseEntity<ApiResponse<Void>>banUser(@PathVariable UUID userId){
        userService.banUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null,"User banned"));
    }

    @PatchMapping("/{userId}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unban a user  (Admin only)")
    public ResponseEntity<ApiResponse<Void>>unbanUser(@PathVariable UUID userId){
        userService.unbanUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null,"User unbanned"));
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change user role  (Admin only)")
    public ResponseEntity<ApiResponse<Void>>changeRole(
            @PathVariable UUID userId,
            @RequestParam UserRole role){
        userService.changeRole(userId, role);
        return ResponseEntity.ok(ApiResponse.success(null,"Role updated"));
    }

}


