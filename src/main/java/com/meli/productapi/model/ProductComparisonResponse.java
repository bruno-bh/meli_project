package com.meli.productapi.model;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO para resposta de comparação de produtos
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
     * Retorna número de produtos sendo comparados
     */
    public int getProductCount() {
        return products != null ? products.size() : 0;
    }
    
    /**
     * Retorna número de campos comparados
     */
    public int getFieldCount() {
        return appliedFilters != null ? appliedFilters.size() : 0;
    }
}
