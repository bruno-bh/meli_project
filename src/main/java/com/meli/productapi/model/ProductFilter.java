package com.meli.productapi.model;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para filtros de busca de produtos
 * Encapsula todos os parâmetros de busca com validações
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilter {
    
    /**
     * Filtro por nome do produto (busca parcial, case-insensitive)
     */
    private String name;
    
    /**
     * Filtro por descrição do produto (busca parcial, case-insensitive)
     */
    private String description;
    
    /**
     * Filtro por tipo do produto (busca exata)
     */
    private String type;
    
    /**
     * Preço mínimo (deve ser maior que zero)
     */
    @Min(value = 1, message = "Preço mínimo deve ser maior que zero")
    private Double priceMin;
    
    /**
     * Preço máximo (deve ser maior que zero)
     */
    @Min(value = 1, message = "Preço máximo deve ser maior que zero")
    private Double priceMax;
    
    /**
     * Número da página (deve ser maior ou igual a 1)
     */
    @Min(value = 1, message = "Número da página deve ser maior ou igual a 1")
    private Integer page;
    
    /**
     * Tamanho da página (deve ser maior que zero)
     */
    @Min(value = 1, message = "Tamanho da página deve ser maior que zero")
    private Integer pageSize;
    
    /**
     * Valida se priceMax >= priceMin
     */
    public void validate() {
        if (priceMin != null && priceMax != null && priceMax < priceMin) {
            throw new IllegalArgumentException("Preço máximo não pode ser menor que o preço mínimo");
        }
    }
}
