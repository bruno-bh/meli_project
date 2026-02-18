package com.meli.productapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for product search filters.
 * Encapsulates all query parameters with validations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Query parameters for product search and pagination")
public class ProductFilter {
    
    /**
     * Filter by product name (partial, case-insensitive search)
     */
    @Schema(description = "Partial, case-insensitive product name search", example = "iPhone")
    private String name;
    
    /**
     * Filter by product type (exact match)
     */
    @Schema(description = "Exact product type match", example = "CELLPHONES")
    private String type;
    
    /**
     * Filter by specification key-value pairs (partial, case-insensitive for text values)
     */
    private Map<String, String> specifications;
    
    /**
     * Minimum price (must be greater than zero)
     */
    @Schema(description = "Minimum price (inclusive, >= 1)", example = "100")
    @Min(value = 1, message = "Minimum price must be greater than zero")
    private Double priceMin;
    
    /**
     * Maximum price (must be greater than zero)
     */
    @Schema(description = "Maximum price (inclusive, >= 1)", example = "5000")
    @Min(value = 1, message = "Maximum price must be greater than zero")
    private Double priceMax;
    
    /**
     * Page number (must be greater than or equal to 1)
     */
    @Schema(description = "Page number (>= 1)", example = "1")
    @Min(value = 1, message = "Page number must be greater than or equal to 1")
    private Integer page;
    
    /**
     * Page size (must be greater than zero)
     */
    @Schema(description = "Items per page (>= 1)", example = "10")
    @Min(value = 1, message = "Page size must be greater than zero")
    private Integer pageSize;
    
    /**
     * Validates that priceMax >= priceMin
     */
    public void validate() {
        if (priceMin != null && priceMax != null && priceMax < priceMin) {
            throw new IllegalArgumentException("Maximum price cannot be less than the minimum price");
        }
    }
}
