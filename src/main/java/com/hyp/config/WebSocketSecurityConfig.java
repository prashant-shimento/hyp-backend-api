package com.hyp.config;

import com.hyp.security.jwt.JwtTokenProvider;
import com.hyp.security.principal.UserPrincipal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                    boolean requiresAuth = sessionAttrs != null
                            && Boolean.TRUE.equals(sessionAttrs.get(WebSocketConfig.REQUIRES_AUTH_ATTR));

                    if (!requiresAuth) {
                        // v2 endpoint — no auth required
                        return message;
                    }

                    // v3 endpoint — validate JWT
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        throw new MessageDeliveryException(
                                "WebSocket connection rejected: Authorization header required in STOMP CONNECT frame");
                    }

                    String token = authHeader.substring(7);
                    UserPrincipal principal = jwtTokenProvider.validateAndExtractAccessPrincipal(token);
                    if (principal == null) {
                        throw new MessageDeliveryException(
                                "WebSocket connection rejected: " + jwtTokenProvider.getJwtFailureReason(token));
                    }

                    accessor.setUser(
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
                    log.debug(
                            "WebSocket v3 authenticated: user={} role={}", principal.getUserId(), principal.getRole());
                }

                return message;
            }
        });
    }
}
