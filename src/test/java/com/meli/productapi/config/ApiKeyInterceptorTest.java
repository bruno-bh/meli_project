package com.meli.productapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meli.productapi.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiKeyInterceptor — API key security tests")
class ApiKeyInterceptorTest {

    private static final String VALID_API_KEY = "meli-product-api-key-2025";
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    private ApiKeyInterceptor createInterceptor(boolean enabled, String key) {
        return new ApiKeyInterceptor(enabled, key, objectMapper);
    }

    @Test
    @DisplayName("Should allow request with valid API key")
    void testValidApiKey_ShouldAllowRequest() throws Exception {
        ApiKeyInterceptor interceptor = createInterceptor(true, VALID_API_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addHeader("X-API-KEY", VALID_API_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result, "Request with valid API key should be allowed");
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Should return 401 when API key is missing")
    void testMissingApiKey_ShouldReturn401() throws Exception {
        ApiKeyInterceptor interceptor = createInterceptor(true, VALID_API_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        // No X-API-KEY header
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result, "Request without API key should be rejected");
        assertEquals(401, response.getStatus());

        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals("Unauthorized", errorResponse.getError());
        assertTrue(errorResponse.getMessage().contains("Missing API key"));
    }

    @Test
    @DisplayName("Should return 401 when API key is invalid")
    void testInvalidApiKey_ShouldReturn401() throws Exception {
        ApiKeyInterceptor interceptor = createInterceptor(true, VALID_API_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        request.addHeader("X-API-KEY", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result, "Request with invalid API key should be rejected");
        assertEquals(401, response.getStatus());

        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals("Unauthorized", errorResponse.getError());
        assertTrue(errorResponse.getMessage().contains("Invalid API key"));
    }

    @Test
    @DisplayName("Should bypass OPTIONS requests (CORS preflight)")
    void testOptionsRequest_ShouldBypass() throws Exception {
        ApiKeyInterceptor interceptor = createInterceptor(true, VALID_API_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/products");
        // No X-API-KEY header
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result, "OPTIONS request should bypass security");
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Should allow request without key when security is disabled")
    void testSecurityDisabled_ShouldAllowWithoutKey() throws Exception {
        ApiKeyInterceptor interceptor = createInterceptor(false, VALID_API_KEY);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        // No X-API-KEY header
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result, "Request should be allowed when security is disabled");
        assertEquals(200, response.getStatus());
    }
}
