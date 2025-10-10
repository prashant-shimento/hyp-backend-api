package com.hyp.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Configuration
public class LoggingFilterConfig extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        filterChain.doFilter(requestWrapper, responseWrapper);
        long timeTaken = System.currentTimeMillis() - startTime;
        String wsEndpoint = "/api/v2/ws";
        if ((HttpMethod.POST.name().equalsIgnoreCase(request.getMethod())
                        || HttpMethod.PATCH.name().equalsIgnoreCase(request.getMethod()))
                && !request.getRequestURI().contains(wsEndpoint)) {

            String requestBody = getRequestPayload(requestWrapper);
            String responseBody = getResponsePayload(responseWrapper);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJsonNode = objectMapper.readTree(responseBody);

            Map<String, Object> logData = new HashMap<>();
            logData.put("method", request.getMethod());
            logData.put("uri", request.getRequestURI());
            logData.put("requestPayload", objectMapper.readTree(requestBody));
            logData.put("responseStatus", response.getStatus());
            logData.put("responsePayload", responseJsonNode);
            logData.put("timeTaken", timeTaken);
            String logJson = objectMapper.writeValueAsString(logData);

            log.info("Request and Response Processed: {}", logJson);
        }

        responseWrapper.copyBodyToResponse();
    }

    private String getRequestPayload(ContentCachingRequestWrapper requestWrapper) throws IOException {
        byte[] contentAsByteArray = requestWrapper.getContentAsByteArray();
        if (contentAsByteArray.length > 0) {
            return new String(contentAsByteArray, StandardCharsets.UTF_8);
        }
        return "";
    }

    private String getResponsePayload(ContentCachingResponseWrapper responseWrapper) throws IOException {
        byte[] contentAsByteArray = responseWrapper.getContentAsByteArray();
        if (contentAsByteArray.length > 0) {
            return new String(contentAsByteArray, StandardCharsets.UTF_8);
        }
        return "";
    }
}
