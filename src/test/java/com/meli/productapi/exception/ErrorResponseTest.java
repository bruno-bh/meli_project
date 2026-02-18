package com.meli.productapi.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ErrorResponse DTO.
 */
@DisplayName("ErrorResponse — error DTO tests")
class ErrorResponseTest {

    @Test
    @DisplayName("Should create ErrorResponse with of() factory method")
    void testOfFactoryMethod() {
        ErrorResponse response = ErrorResponse.of(
                400, "Bad Request", "Invalid input", "/api/v1/products");

        assertNotNull(response);
        assertNotNull(response.getTimestamp());
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getError());
        assertEquals("Invalid input", response.getMessage());
        assertEquals("/api/v1/products", response.getPath());
        assertNull(response.getFieldErrors());
    }

    @Test
    @DisplayName("Should create ErrorResponse with ofValidation() factory method")
    void testOfValidationFactoryMethod() {
        Map<String, String> fieldErrors = Map.of(
                "name", "must not be blank",
                "price", "must be positive"
        );

        ErrorResponse response = ErrorResponse.ofValidation(
                400, "Bad Request", fieldErrors, "/api/v1/products");

        assertNotNull(response);
        assertNotNull(response.getTimestamp());
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getError());
        assertNull(response.getMessage());
        assertEquals("/api/v1/products", response.getPath());
        assertNotNull(response.getFieldErrors());
        assertEquals(2, response.getFieldErrors().size());
        assertEquals("must not be blank", response.getFieldErrors().get("name"));
    }

    @Test
    @DisplayName("Should create ErrorResponse with builder")
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(now)
                .status(404)
                .error("Not Found")
                .message("Product not found")
                .path("/api/v1/products/999")
                .build();

        assertEquals(now, response.getTimestamp());
        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getError());
        assertEquals("Product not found", response.getMessage());
        assertEquals("/api/v1/products/999", response.getPath());
        assertNull(response.getFieldErrors());
    }

    @Test
    @DisplayName("Should create ErrorResponse with no-args constructor")
    void testNoArgsConstructor() {
        ErrorResponse response = new ErrorResponse();

        assertNull(response.getTimestamp());
        assertEquals(0, response.getStatus());
        assertNull(response.getError());
        assertNull(response.getMessage());
        assertNull(response.getPath());
        assertNull(response.getFieldErrors());
    }

    @Test
    @DisplayName("Should create ErrorResponse with all-args constructor")
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> fieldErrors = Map.of("field", "error");

        ErrorResponse response = new ErrorResponse(
                now, 500, "Internal Server Error", "Something went wrong",
                "/api/v1/products", fieldErrors);

        assertEquals(now, response.getTimestamp());
        assertEquals(500, response.getStatus());
        assertEquals("Internal Server Error", response.getError());
        assertEquals("Something went wrong", response.getMessage());
        assertEquals("/api/v1/products", response.getPath());
        assertEquals(fieldErrors, response.getFieldErrors());
    }
}
