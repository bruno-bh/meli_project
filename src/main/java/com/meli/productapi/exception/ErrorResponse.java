package com.meli.productapi.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized DTO for API error responses.
 * Ensures a consistent structure across all error responses.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response")
public class ErrorResponse {

    @Schema(description = "Timestamp of the error")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error category", example = "Bad Request")
    private String error;

    @Schema(description = "Detailed error message")
    private String message;

    @Schema(description = "Request path that caused the error", example = "/api/v1/products")
    private String path;

    @Schema(description = "Field-level validation errors")
    private Map<String, String> fieldErrors;

    /**
     * Creates a simple ErrorResponse with a message.
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Creates an ErrorResponse with field-level validation errors.
     */
    public static ErrorResponse ofValidation(int status, String error, Map<String, String> fieldErrors, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .fieldErrors(fieldErrors)
                .path(path)
                .build();
    }
}
