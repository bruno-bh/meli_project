package com.meli.productapi.model;

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
public class ProductFilter {
    
    /**
     * Filter by product name (partial, case-insensitive search)
     */
    private String name;
    
    /**
     * Filter by product type (exact match)
     */
    private String type;
    
    /**
     * Filter by specification key-value pairs (partial, case-insensitive for text values)
     */
    private Map<String, String> specifications;
    
    /**
     * Minimum price (must be greater than zero)
     */
    @Min(value = 1, message = "Minimum price must be greater than zero")
    private Double priceMin;
    
    /**
     * Maximum price (must be greater than zero)
     */
    @Min(value = 1, message = "Maximum price must be greater than zero")
    private Double priceMax;
    
    /**
     * Page number (must be greater than or equal to 1)
     */
    @Min(value = 1, message = "Page number must be greater than or equal to 1")
    private Integer page;
    
    /**
     * Page size (must be greater than zero)
     */
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
