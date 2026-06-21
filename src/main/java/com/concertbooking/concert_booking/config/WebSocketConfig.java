package com.concertbooking.concert_booking.config;


import com.concertbooking.concert_booking.auth.filter.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketPrincipalHandshakeHandler webSocketPrincipalHandshakeHandler;
    @Value("${app.frontend.url}")
    private String frontendUrl;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");


        registry.enableStompBrokerRelay("/topic")
                .setRelayHost("rabbitmq")
                .setRelayPort(61613) // Default external STOMP port
                .setClientLogin("concert_user")
                .setClientPasscode("concert_pass")
                .setSystemLogin("concert_user")
                .setSystemPasscode("concert_pass")
                .setVirtualHost("/");



    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Frontend connects here for websocket
        registry.addEndpoint("/ws")
                .setAllowedOrigins(frontendUrl)
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(webSocketPrincipalHandshakeHandler)
                .withSockJS();
    }

}
