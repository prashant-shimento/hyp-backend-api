package com.hyp.config;

import com.hyp.security.apikey.ApiKeyAuthenticationFilter;
import com.hyp.security.audit.AuditLoggingFilter;
import com.hyp.security.jwt.JwtAccessDeniedHandler;
import com.hyp.security.jwt.JwtAuthenticationEntryPoint;
import com.hyp.security.jwt.JwtAuthenticationFilter;
import com.hyp.security.policy.PolicyEnforcementFilter;
import com.hyp.security.ratelimit.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final PolicyEnforcementFilter policyEnforcementFilter;
    private final AuditLoggingFilter auditLoggingFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))

                // Authorization - PolicyEnforcementFilter handles all authorization logic
                // This allows requests through; actual authorization is done in PolicyEnforcementFilter
                .authorizeHttpRequests(auth -> auth
                        // Health check and actuator endpoints
                        .requestMatchers("/actuator/health/**")
                        .permitAll()
                        // Swagger/OpenAPI documentation
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                        .permitAll()
                        // Error endpoints
                        .requestMatchers("/error")
                        .permitAll()
                        // All other requests go through policy filter
                        .anyRequest()
                        .permitAll())

                // Filter chain order (chained relative to each other for guaranteed ordering):
                // 1. RateLimitingFilter - Rate limit check first
                // 2. ApiKeyAuthenticationFilter - Authenticate webhook requests (API key + IP/HMAC)
                // 3. JwtAuthenticationFilter - Authenticate JWT tokens
                // 4. AuditLoggingFilter - Log all requests with principal context (before policy so denials are
                // captured)
                // 5. PolicyEnforcementFilter - Authorize based on permissions
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(apiKeyAuthenticationFilter, RateLimitingFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, ApiKeyAuthenticationFilter.class)
                .addFilterAfter(auditLoggingFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(policyEnforcementFilter, AuditLoggingFilter.class)
                .build();
    }
}
