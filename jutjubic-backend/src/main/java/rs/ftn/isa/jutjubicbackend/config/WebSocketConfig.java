package rs.ftn.isa.jutjubicbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        log.info("Configuring WebSocket message broker");
        // Enable a simple in-memory message broker to send messages to clients
        config.enableSimpleBroker("/topic");
        // Prefix for messages from clients
        config.setApplicationDestinationPrefixes("/app");
        log.info("WebSocket message broker configured - Simple broker: /topic, App destination prefix: /app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registering WebSocket STOMP endpoints");
        // Register STOMP endpoint that clients will connect to
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        log.info("WebSocket STOMP endpoint registered: /ws with SockJS support and allowed origin patterns: *");
    }
}

