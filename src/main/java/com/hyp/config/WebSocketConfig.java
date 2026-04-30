package com.hyp.config;

import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    static final String REQUIRES_AUTH_ATTR = "ws.requiresAuth";

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic/");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // v2 endpoint — no authentication required (legacy clients)
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();

        // v3 endpoint — JWT required in STOMP CONNECT frame
        registry.addEndpoint("/v3/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(
                            ServerHttpRequest request,
                            ServerHttpResponse response,
                            WebSocketHandler wsHandler,
                            Map<String, Object> attributes) {
                        attributes.put(REQUIRES_AUTH_ATTR, true);
                        return true;
                    }

                    @Override
                    public void afterHandshake(
                            ServerHttpRequest request,
                            ServerHttpResponse response,
                            WebSocketHandler wsHandler,
                            Exception exception) {}
                })
                .withSockJS();
    }
}
