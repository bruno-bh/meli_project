package com.meli.productapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meli.productapi.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final boolean securityEnabled;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyInterceptor(
            @Value("${api.security.enabled:true}") boolean securityEnabled,
            @Value("${api.security.key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.securityEnabled = securityEnabled;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Bypass when security is disabled
        if (!securityEnabled) {
            return true;
        }

        // Bypass CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestApiKey = request.getHeader(API_KEY_HEADER);

        if (requestApiKey == null || requestApiKey.isBlank()) {
            log.warn("Missing API key on request to {}", request.getRequestURI());
            writeUnauthorizedResponse(response, request, "Missing API key. Provide a valid key via the X-API-KEY header.");
            return false;
        }

        if (!apiKey.equals(requestApiKey)) {
            log.warn("Invalid API key on request to {}", request.getRequestURI());
            writeUnauthorizedResponse(response, request, "Invalid API key.");
            return false;
        }

        return true;
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, HttpServletRequest request, String message) throws Exception {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                message,
                request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
