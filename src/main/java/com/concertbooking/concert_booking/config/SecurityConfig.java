package com.concertbooking.concert_booking.config;

import com.concertbooking.concert_booking.auth.filter.JwtAuthFilter;
import com.concertbooking.concert_booking.auth.oauth2.OAuth2SuccessHandler;
import com.concertbooking.concert_booking.auth.service.CustomUserDetailsService;
import com.concertbooking.concert_booking.auth.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomUserDetailsService userDetailsService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    //public urls which are allowed
    private static final String[] PUBLIC_URLS={
            "/api/v1/auth/**",
            "/api/v1/concerts",
            "/api/v1/concerts/**",
            "/api/v1/categories/**",
            "/api/v1/venues/**",
            "/api/v1/payments/webhook",
            "/api/v1/seats/concert/*/viewing",
            "/api/v1/queue/status/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/ws/**" ,
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus",
            "/api/v1/concerts/organizer/**",
            "/api/v1/reviews/organizer/**"


    };
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider=new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    //CORS CONFIGS
    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration config=new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of(
                "GET","POST","PUT","DELETE","PATCH","OPTIONS"
        ));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Idempotency-Key",
                "X-Session-Id"
        ));

        config.setExposedHeaders(List.of(
                "Authorization"
        ));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",config);
        return source;

    }
    //main rules:
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
                //1) CSRF disable
                .csrf(AbstractHttpConfigurer::disable)

                //2) Allow frontend reqs as CORS generally doesnt allows frontend url to comms with backend url
                .cors(cors-> cors.configurationSource(corsConfigurationSource()))

                //3)STATELESS sessions,every req brings jwt with it
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                (request, response, authException) ->{
                                    response.sendError(
                                            HttpServletResponse.SC_UNAUTHORIZED,
                                            "Unauthorized"
                                    );
                                }
                        )
                )
                //4)SECURITY HEADERS:
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(content -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self' https://accounts.google.com https://apis.google.com; " +
                                                "style-src 'self' 'unsafe-inline'; " +
                                                "img-src 'self' data: https:; " +
                                                "connect-src 'self' " +
                                                "ws://localhost:8080 " +
                                                "wss://localhost:8080 " +
                                                "https://accounts.google.com " +
                                                "https://oauth2.googleapis.com; " +
                                                "frame-src https://accounts.google.com; " +
                                                "object-src 'none'; " +
                                                "frame-ancestors 'none';"
                                )
                        )
                )
                //5)AUTH RULES
                .authorizeHttpRequests(auth->auth
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        //Only ADMIN
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/{userId}").hasRole("ADMIN")

                        //Only organizer or admin can create update and del concerts
                        .requestMatchers(HttpMethod.POST,"/api/v1/concerts/**").hasAnyRole("ORGANIZER","ADMIN")
                        .requestMatchers(HttpMethod.PUT,"/api/v1/concerts/**").hasAnyRole("ORGANIZER","ADMIN")
                        .requestMatchers(HttpMethod.DELETE,"/api/v1/concerts/**").hasAnyRole("ORGANIZER","ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/checkin").hasAnyRole("ADMIN", "ORGANIZER")
                        //Venue creation done by only ADMIN

                        .requestMatchers(HttpMethod.POST,"/api/v1/venues/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,"/api/v1/venues/**").hasRole("ADMIN")
                        //oauth
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        //Now excluding the above everyone else must be logged in
                        .anyRequest().authenticated()

                )
                //OAUTH 2 Login--> Google Authenticates--> Oauth2SuccessHandler called-> JWT generated
                .oauth2Login(oauth2->oauth2
                        .successHandler(oAuth2SuccessHandler)
                )

                .authenticationProvider(authenticationProvider())

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
