package com.concertbooking.concert_booking.auth.controller;


import com.concertbooking.concert_booking.auth.dto.*;
import com.concertbooking.concert_booking.auth.service.AuthService;
import com.concertbooking.concert_booking.common.response.ApiResponse;
import com.concertbooking.concert_booking.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.nio.file.LinkOption;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name="Authentication",description="Register, login, OTP verification")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = " Register and send OTP to email")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request){
        authService.initiateRegistration(request);
        return  ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Registration initiated. Check your email for OTP."));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify OTP and activate account")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(@Valid @RequestBody  OtpVerifyRequest request) {
            AuthResponse response=authService.verifyEmailAndActivate(request);
            return ResponseEntity.ok(ApiResponse.success(response,"Email verified Successfully"));


    }
    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>>login(@Valid @RequestBody LoginRequest request){
        AuthResponse response=authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response,"Login successfull"));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Get new access token with help of refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request){
        AuthResponse response=authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response,"Token refreshed"));

    }
    @PostMapping("/resend-otp")
    @Operation(summary = "Resend the otp to given email")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestParam String email){
        authService.resendOtp(email);
        return ResponseEntity.ok(ApiResponse.success(null,"OTP has been resent"));
    }
    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate access token")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader,@AuthenticationPrincipal User user) {
        String token=authHeader.substring(7);
        authService.logout(token,user);
        return ResponseEntity.ok(ApiResponse.success(null,"Logged out successfully"));
    }
    @PostMapping("/oauth2/exchange")
    @Operation(summary = "Exchange OAuth2 code for tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> exchangeCode(@Valid @RequestBody ExchangeCodeRequest request) {
        AuthResponse response = authService.exchangeOAuthCode(request.getCode());
        return ResponseEntity.ok(ApiResponse.success(response, "Token exchange successful"));
    }
    }



