package com.meli.productapi.model;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO for product comparison response.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductComparisonResponse {
    
    private String productType;
    private List<String> appliedFilters;
    private List<Map<String, Object>> products;
    
    /**
     * Returns the number of products being compared.
     */
    public int getProductCount() {
        return products != null ? products.size() : 0;
    }
    
    /**
     * Returns the number of fields being compared.
     */
    public int getFieldCount() {
        return appliedFilters != null ? appliedFilters.size() : 0;
    }
}
