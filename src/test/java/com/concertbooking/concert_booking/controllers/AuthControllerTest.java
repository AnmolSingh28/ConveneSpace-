package com.concertbooking.concert_booking.controllers;
import com.concertbooking.concert_booking.auth.controller.AuthController;
import com.concertbooking.concert_booking.auth.dto.*;
import com.concertbooking.concert_booking.auth.filter.JwtAuthFilter;
import com.concertbooking.concert_booking.auth.oauth2.OAuth2SuccessHandler;
import com.concertbooking.concert_booking.auth.service.AuthService;
import com.concertbooking.concert_booking.auth.service.CustomUserDetailsService;
import com.concertbooking.concert_booking.auth.util.JwtUtil;
import com.concertbooking.concert_booking.common.enums.UserRole;
import com.concertbooking.concert_booking.common.exception.AuthException;
import com.concertbooking.concert_booking.common.exception.GlobalExceptionHandler;
import com.concertbooking.concert_booking.common.exception.OtpException;
import com.concertbooking.concert_booking.config.RateLimiting;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private RateLimiting rateLimiting;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;
    private AuthResponse sampleAuthResponse;

    @BeforeEach
    void setUp() {
        sampleAuthResponse = AuthResponse.builder()
                .accessToken("access-token-xyz")
                .refreshToken("refresh-token-xyz")
                .tokenType("Bearer")
                .expiresIn(3600000L)
                .userId(UUID.randomUUID())
                .name("Gogi Test")
                .email("gogi@example.com")
                .role("USER")
                .build();
    }

    @Nested
    @DisplayName("POST /register")
    class Register {

        private RegisterRequest validRequest() {
            RegisterRequest req = new RegisterRequest();
            req.setName("Gogi Test");
            req.setEmail("gogi@example.com");
            req.setPassword("Password1");
            req.setPhone("9876543210");
            req.setRole(UserRole.USER);
            return req;
        }

        @Test
        @DisplayName("201 - valid registration triggers OTP")
        void register_validRequest_returns201() throws Exception {
            doNothing().when(authService).initiateRegistration(any());

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Registration initiated. Check your email for OTP."))
                    .andExpect(jsonPath("$.data").isEmpty());

            verify(authService).initiateRegistration(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("400 - blank name fails validation")
        void register_blankName_returns400() throws Exception {
            RegisterRequest req = validRequest();
            req.setName("");

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data.name").exists());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - invalid email format fails validation")
        void register_invalidEmail_returns400() throws Exception {
            RegisterRequest req = validRequest();
            req.setEmail("not-an-email");

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.email").exists());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - weak password fails pattern validation")
        void register_weakPassword_returns400() throws Exception {
            RegisterRequest req = validRequest();
            req.setPassword("alllowercase");

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.password").exists());
        }

        @Test
        @DisplayName("400 - password too short fails size validation")
        void register_shortPassword_returns400() throws Exception {
            RegisterRequest req = validRequest();
            req.setPassword("Ab1");

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.password").exists());
        }

        @Test
        @DisplayName("400 - invalid Indian phone number fails validation")
        void register_invalidPhone_returns400() throws Exception {
            RegisterRequest req = validRequest();
            req.setPhone("1234567890");  // doesn't start with 6-9

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.phone").exists());
        }

        @Test
        @DisplayName("400 - null role fails validation")
        void register_nullRole_returns400() throws Exception {
            RegisterRequest req = validRequest();
            req.setRole(null);

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.role").exists());
        }

        @Test
        @DisplayName("409 - AuthException from service propagates correctly")
        void register_duplicateEmail_returnsConflict() throws Exception {
            doThrow(new AuthException("Email already registered"))
                    .when(authService).initiateRegistration(any());

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnauthorized())  // AuthException → 401
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email already registered"));
        }

        @Test
        @DisplayName("201 - phone field is optional (blank allowed)")
        void register_blankPhone_isAllowed() throws Exception {
            RegisterRequest req = validRequest();
            req.setPhone("");

            doNothing().when(authService).initiateRegistration(any());

            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }
    }


    @Nested
    @DisplayName("POST /verify-email")
    class VerifyEmail {
        private OtpVerifyRequest validRequest() {
            OtpVerifyRequest req = new OtpVerifyRequest();
            req.setEmail("gogi@example.com");
            req.setOtp("123456");
            return req;
        }

        @Test
        @DisplayName("200 - valid OTP activates account and returns tokens")
        void verifyEmail_validOtp_returns200WithTokens() throws Exception {
            when(authService.verifyEmailAndActivate(any())).thenReturn(sampleAuthResponse);

            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Email verified Successfully"))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token-xyz"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.email").value("gogi@example.com"));

            verify(authService).verifyEmailAndActivate(any(OtpVerifyRequest.class));
        }

        @Test
        @DisplayName("400 - OTP less than 6 digits fails validation")
        void verifyEmail_shortOtp_returns400() throws Exception {
            OtpVerifyRequest req = validRequest();
            req.setOtp("123");

            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.otp").exists());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - OTP more than 6 digits fails validation")
        void verifyEmail_longOtp_returns400() throws Exception {
            OtpVerifyRequest req = validRequest();
            req.setOtp("1234567");

            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.otp").exists());
        }

        @Test
        @DisplayName("400 - blank email fails validation")
        void verifyEmail_blankEmail_returns400() throws Exception {
            OtpVerifyRequest req = validRequest();
            req.setEmail("");

            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.email").exists());
        }

        @Test
        @DisplayName("400 - expired/invalid OTP → OtpException")
        void verifyEmail_invalidOtp_returnsOtpError() throws Exception {
            when(authService.verifyEmailAndActivate(any()))
                    .thenThrow(new OtpException("OTP expired or invalid"));

            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("OTP expired or invalid"));
        }
    }


    @Nested
    @DisplayName("POST /login")
    class Login {
        private LoginRequest validRequest() {
            LoginRequest req = new LoginRequest();
            req.setEmail("gogi@example.com");
            req.setPassword("Password1");
            return req;
        }

        @Test
        @DisplayName("200 - valid credentials returns tokens")
        void login_validCredentials_returns200() throws Exception {
            when(authService.login(any())).thenReturn(sampleAuthResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Login successfull"))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token-xyz"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-xyz"))
                    .andExpect(jsonPath("$.data.role").value("USER"));

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("400 - blank email fails validation")
        void login_blankEmail_returns400() throws Exception {
            LoginRequest req = validRequest();
            req.setEmail("");

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.email").exists());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - invalid email format fails validation")
        void login_invalidEmailFormat_returns400() throws Exception {
            LoginRequest req = validRequest();
            req.setEmail("bad-email");

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.email").exists());
        }

        @Test
        @DisplayName("400 - blank password fails validation")
        void login_blankPassword_returns400() throws Exception {
            LoginRequest req = validRequest();
            req.setPassword("");

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.password").exists());
        }

        @Test
        @DisplayName("401 - wrong password → AuthException")
        void login_wrongPassword_returns401() throws Exception {
            when(authService.login(any())).thenThrow(new AuthException("Invalid email or password"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("401 - unverified account → AuthException")
        void login_unverifiedAccount_returns401() throws Exception {
            when(authService.login(any())).thenThrow(new AuthException("Account not verified. Please verify email first."));

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Account not verified. Please verify email first."));
        }
    }


    @Nested
    @DisplayName("POST /refresh-token")
    class RefreshToken {

        private RefreshTokenRequest validRequest() {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("valid-refresh-token");
            return req;
        }

        @Test
        @DisplayName("200 - valid refresh token returns new tokens")
        void refreshToken_valid_returns200() throws Exception {
            when(authService.refreshToken(any())).thenReturn(sampleAuthResponse);

            mockMvc.perform(post("/api/v1/auth/refresh-token")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Token refreshed"))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token-xyz"));

            verify(authService).refreshToken(any(RefreshTokenRequest.class));
        }

        @Test
        @DisplayName("400 - blank refresh token fails validation")
        void refreshToken_blank_returns400() throws Exception {
            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("");

            mockMvc.perform(post("/api/v1/auth/refresh-token")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.refreshToken").exists());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("400 - null refresh token fails validation")
        void refreshToken_null_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh-token")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\": null}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("401 - expired refresh token → AuthException")
        void refreshToken_expired_returns401() throws Exception {
            when(authService.refreshToken(any())).thenThrow(new AuthException("Refresh token expired"));

            mockMvc.perform(post("/api/v1/auth/refresh-token")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Refresh token expired"));
        }
    }


    @Nested
    @DisplayName("POST /resend-otp")
    class ResendOtp {

        @Test
        @DisplayName("200 - valid email resends OTP")
        void resendOtp_validEmail_returns200() throws Exception {
            doNothing().when(authService).resendOtp(any());

            mockMvc.perform(post("/api/v1/auth/resend-otp")
                            .with(csrf())
                            .param("email", "gogi@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("OTP has been resent"))
                    .andExpect(jsonPath("$.data").isEmpty());

            verify(authService).resendOtp(eq("gogi@example.com"));
        }

        @Test
        @DisplayName("400 - OtpException when email not registered")
        void resendOtp_unregisteredEmail_returnsOtpError() throws Exception {
            doThrow(new OtpException("No pending registration for this email"))
                    .when(authService).resendOtp(any());

            mockMvc.perform(post("/api/v1/auth/resend-otp")
                            .with(csrf())
                            .param("email", "notfound@example.com"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("No pending registration for this email"));
        }

        @Test
        @DisplayName("400 - AuthException when already verified")
        void resendOtp_alreadyVerified_returns401() throws Exception {
            doThrow(new AuthException("Account already verified"))
                    .when(authService).resendOtp(any());

            mockMvc.perform(post("/api/v1/auth/resend-otp")
                            .with(csrf())
                            .param("email", "gogi@example.com"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Account already verified"));
        }
    }
}
