package com.meli.productapi.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.meli.productapi.controller.ProductController;
import com.meli.productapi.service.ProductService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for GlobalExceptionHandler — validates that each exception type
 * is translated into the correct HTTP status and structured ErrorResponse.
 */
@WebMvcTest(ProductController.class)
@DisplayName("GlobalExceptionHandler — error response mapping tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    // ── HttpMessageNotReadableException (malformed JSON) ─────────────────

    @Test
    @DisplayName("Should return 400 for malformed JSON body")
    void testHandleHttpMessageNotReadable() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    // ── HttpMediaTypeNotSupportedException ───────────────────────────────

    @Test
    @DisplayName("Should return error for unsupported content type")
    void testHandleHttpMediaTypeNotSupported() throws Exception {
        // Spring may return 415 before the handler or 400 via the handler.
        // Both are valid — we just verify it's not a 2xx/5xx.
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text body"))
                .andExpect(status().is4xxClientError());
    }

    // ── MethodArgumentTypeMismatchException ──────────────────────────────

    @Test
    @DisplayName("Should return 400 for type mismatch query parameter")
    void testHandleTypeMismatch() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("page", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    // ── IllegalArgumentException ─────────────────────────────────────────

    @Test
    @DisplayName("Should return 400 for IllegalArgumentException")
    void testHandleIllegalArgumentException() throws Exception {
        when(productService.createProduct(any()))
                .thenThrow(new IllegalArgumentException("Product name is required"));

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":null,\"price\":{\"value\":10,\"unit\":\"BRL\"},\"type\":\"CELLPHONES\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Product name is required"));
    }

    // ── ProductNotFoundException ─────────────────────────────────────────

    @Test
    @DisplayName("Should return 404 for ProductNotFoundException")
    void testHandleProductNotFoundException() throws Exception {
        when(productService.getProductById("999"))
                .thenThrow(new ProductNotFoundException("Product with ID 999 not found"));

        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product with ID 999 not found"));
    }

    // ── IncompatibleProductTypesException ─────────────────────────────────

    @Test
    @DisplayName("Should return 409 for IncompatibleProductTypesException")
    void testHandleIncompatibleProductTypesException() throws Exception {
        when(productService.compareProducts(any(), any()))
                .thenThrow(new IncompatibleProductTypesException("Products must have the same type"));

        mockMvc.perform(get("/api/v1/products/compare")
                .param("ids", "1,2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Products must have the same type"));
    }

    // ── Generic Exception (500) ──────────────────────────────────────────

    @Test
    @DisplayName("Should return 500 for unhandled generic exception")
    void testHandleGlobalException() throws Exception {
        when(productService.getProductById("crash"))
                .thenThrow(new RuntimeException("Unexpected internal error"));

        mockMvc.perform(get("/api/v1/products/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    // ── ConstraintViolationException (via Bean Validation @Min) ──────────

    @Test
    @DisplayName("Should return 400 for Bean Validation constraint violation on query params")
    void testHandleConstraintViolation() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("pageSize", "0"))
                .andExpect(status().isBadRequest());
    }

    // ── MethodArgumentNotValidException ──────────────────────────────────
    // Note: This handler handles @Valid @RequestBody validation failures.
    // It's triggered via Bean Validation annotations on the Product model
    // (if present). We test it indirectly by checking field-level validation:

    @Test
    @DisplayName("Should return 400 with priceMax < priceMin (filter validation)")
    void testHandlePriceRangeValidation() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("priceMin", "1000")
                .param("priceMax", "500"))
                .andExpect(status().isBadRequest());
    }
}
