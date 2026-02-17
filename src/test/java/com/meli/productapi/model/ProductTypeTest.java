package com.meli.productapi.model;

import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para o enum ProductType
 */
class ProductTypeTest {

    @Test
    void testProductTypeValues() {
        ProductType[] types = ProductType.values();
        assertEquals(8, types.length, "Should have 8 product types");
        
        // Verify all types exist
        assertNotNull(ProductType.CELLPHONES);
        assertNotNull(ProductType.COMPUTERS);
        assertNotNull(ProductType.CLOTHING);
        assertNotNull(ProductType.FOOD);
        assertNotNull(ProductType.BEVERAGES);
        assertNotNull(ProductType.FURNITURE);
        assertNotNull(ProductType.BOOKS);
        assertNotNull(ProductType.SPORTS);
    }

    @Test
    void testDisplayNames() {
        assertEquals("Smartphones", ProductType.CELLPHONES.getDisplayName());
        assertEquals("Computadores", ProductType.COMPUTERS.getDisplayName());
        assertEquals("Roupas", ProductType.CLOTHING.getDisplayName());
        assertEquals("Alimentos", ProductType.FOOD.getDisplayName());
    }

    @Test
    void testMetadataFields() {
        // Test CELLPHONES metadata
        var cellphonesMeta = ProductType.CELLPHONES.getMetadata();
        assertTrue(cellphonesMeta.contains("brand"));
        assertTrue(cellphonesMeta.contains("storage_gb"));
        assertTrue(cellphonesMeta.contains("memory_gb"));
        assertEquals(5, cellphonesMeta.size());

        // Test CLOTHING metadata
        var clothingMeta = ProductType.CLOTHING.getMetadata();
        assertTrue(clothingMeta.contains("size_us"));
        assertTrue(clothingMeta.contains("material"));
        assertEquals(5, clothingMeta.size());

        // Test FOOD metadata
        var foodMeta = ProductType.FOOD.getMetadata();
        assertTrue(foodMeta.contains("manufacturing_date"));
        assertTrue(foodMeta.contains("expiration_date"));
        assertEquals(5, foodMeta.size());
    }

    @Test
    void testFromStringValid() {
        Optional<ProductType> cellphones = ProductType.fromString("CELLPHONES");
        assertTrue(cellphones.isPresent());
        assertEquals(ProductType.CELLPHONES, cellphones.get());

        Optional<ProductType> food = ProductType.fromString("food");
        assertTrue(food.isPresent());
        assertEquals(ProductType.FOOD, food.get());

        Optional<ProductType> clothing = ProductType.fromString("Clothing");
        assertTrue(clothing.isPresent());
        assertEquals(ProductType.CLOTHING, clothing.get());
    }

    @Test
    void testFromStringInvalid() {
        Optional<ProductType> invalid = ProductType.fromString("INVALID_TYPE");
        assertFalse(invalid.isPresent());

        Optional<ProductType> empty = ProductType.fromString("");
        assertFalse(empty.isPresent());

        Optional<ProductType> nullType = ProductType.fromString(null);
        assertFalse(nullType.isPresent());
    }

    @Test
    void testGetAllTypesInfo() {
        String info = ProductType.getAllTypesInfo();
        assertNotNull(info);
        assertTrue(info.contains("CELLPHONES"));
        assertTrue(info.contains("COMPUTERS"));
        assertTrue(info.contains("Smartphones"));
        assertTrue(info.contains("Metadata:"));
    }

    @Test
    void testMetadataImmutable() {
        var metadata = ProductType.CELLPHONES.getMetadata();
        assertThrows(UnsupportedOperationException.class, 
            () -> metadata.add("new_field"),
            "Metadata should be immutable");
    }
}
