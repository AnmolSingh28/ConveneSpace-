package com.concertbooking.concert_booking.auth.oauth2;


import com.concertbooking.concert_booking.auth.util.JwtUtil;
import com.concertbooking.concert_booking.common.enums.UserRole;
import com.concertbooking.concert_booking.user.entity.User;
import com.concertbooking.concert_booking.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private  final JwtUtil jwtUtil;
    @Value("${app.frontend.url}")
    private String frontendUrl;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String oauthId = oAuth2User.getAttribute("sub");

        // Find or create user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .name(name)
                            .email(email)
                            .role(UserRole.USER)
                            .emailVerified(true)
                            .active(true)
                            .oauthProvider("google")
                            .oauthId(oauthId)
                            .build();
                    return userRepository.save(newUser);
                });

        // Update OAuth ID if user existed but didn't have it
        if (user.getOauthId() == null) {
            user.setOauthProvider("google");
            user.setOauthId(oauthId);
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        String exchangeCode = UUID.randomUUID().toString();
        Map<String, String> tokenData = Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "userId", user.getId().toString(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        );
        redisTemplate.opsForValue().set(
                "oauth:exchange:" + exchangeCode,
                objectMapper.writeValueAsString(tokenData),
                Duration.ofSeconds(30)
        );
        // Redirect to frontend with tokens
        String redirectUrl = frontendUrl + "/oauth2/callback?code=" + exchangeCode;

        log.info("OAuth2 login successful for: {}", email);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }


}
