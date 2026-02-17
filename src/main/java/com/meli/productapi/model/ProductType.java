package com.meli.productapi.model;

import java.util.*;

/**
 * Enum que define os tipos de produtos disponíveis
 * Cada tipo possui metadados específicos esperados
 */
public enum ProductType {
    CELLPHONES("Smartphones", List.of("brand", "storage_gb", "memory_gb", "screen_size", "camera_mp")),
    COMPUTERS("Computadores", List.of("brand", "processor", "ram_gb", "storage_gb", "screen_size")),
    CLOTHING("Roupas", List.of("size_us", "size_eu", "material", "composition", "color_variations")),
    FOOD("Alimentos", List.of("manufacturing_date", "expiration_date", "nutriscore", "origin", "weight")),
    BEVERAGES("Bebidas", List.of("volume_ml", "origin", "expiration_date", "ingredients", "alcohol_content")),
    FURNITURE("Móveis", List.of("material", "dimensions", "weight_kg", "color", "warranty_months")),
    BOOKS("Livros", List.of("author", "publisher", "pages", "language", "isbn")),
    SPORTS("Esportes", List.of("size", "material", "color", "technology", "warranty_months"));

    private final String displayName;
    private final List<String> metadata;

    ProductType(String displayName, List<String> metadata) {
        this.displayName = displayName;
        this.metadata = Collections.unmodifiableList(metadata);
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getMetadata() {
        return metadata;
    }

    /**
     * Obtém um ProductType pelo nome
     */
    public static Optional<ProductType> fromString(String type) {
        if (type == null || type.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ProductType.valueOf(type.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Retorna todos os tipos disponíveis com seus nomes e metadados
     */
    public static String getAllTypesInfo() {
        StringBuilder sb = new StringBuilder();
        for (ProductType type : ProductType.values()) {
            sb.append(String.format("%s: %s - Metadata: %s\n", 
                type.name(), type.displayName, type.metadata));
        }
        return sb.toString();
    }
}
