package com.meli.productapi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for ProductFilter's validate() method and builder defaults.
 */
@DisplayName("ProductFilter — filter validation tests")
class ProductFilterTest {

    @Test
    @DisplayName("Should not throw when filter is completely empty")
    void testValidateEmptyFilter() {
        ProductFilter filter = ProductFilter.builder().build();
        assertDoesNotThrow(filter::validate);
    }

    @Test
    @DisplayName("Should not throw when priceMin is set without priceMax")
    void testValidateOnlyPriceMin() {
        ProductFilter filter = ProductFilter.builder()
                .priceMin(100.0)
                .build();
        assertDoesNotThrow(filter::validate);
    }

    @Test
    @DisplayName("Should not throw when priceMax is set without priceMin")
    void testValidateOnlyPriceMax() {
        ProductFilter filter = ProductFilter.builder()
                .priceMax(500.0)
                .build();
        assertDoesNotThrow(filter::validate);
    }

    @Test
    @DisplayName("Should not throw when priceMax equals priceMin")
    void testValidatePriceMaxEqualsPriceMin() {
        ProductFilter filter = ProductFilter.builder()
                .priceMin(100.0)
                .priceMax(100.0)
                .build();
        assertDoesNotThrow(filter::validate);
    }

    @Test
    @DisplayName("Should not throw when priceMax > priceMin")
    void testValidateValidPriceRange() {
        ProductFilter filter = ProductFilter.builder()
                .priceMin(100.0)
                .priceMax(500.0)
                .build();
        assertDoesNotThrow(filter::validate);
    }

    @Test
    @DisplayName("Should throw when priceMax < priceMin")
    void testValidatePriceMaxLessThanPriceMin() {
        ProductFilter filter = ProductFilter.builder()
                .priceMin(1000.0)
                .priceMax(500.0)
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                filter::validate
        );
        assertTrue(exception.getMessage().contains("Maximum price cannot be less than the minimum price"));
    }

    @Test
    @DisplayName("Should preserve all fields via builder")
    void testBuilderPreservesAllFields() {
        ProductFilter filter = ProductFilter.builder()
                .name("iPhone")
                .type("CELLPHONES")
                .priceMin(500.0)
                .priceMax(2000.0)
                .page(2)
                .pageSize(10)
                .build();

        assertEquals("iPhone", filter.getName());
        assertEquals("CELLPHONES", filter.getType());
        assertEquals(500.0, filter.getPriceMin());
        assertEquals(2000.0, filter.getPriceMax());
        assertEquals(2, filter.getPage());
        assertEquals(10, filter.getPageSize());
    }

    @Test
    @DisplayName("Should have null defaults for all fields")
    void testDefaultsAreNull() {
        ProductFilter filter = ProductFilter.builder().build();

        assertNull(filter.getName());
        assertNull(filter.getType());
        assertNull(filter.getSpecifications());
        assertNull(filter.getPriceMin());
        assertNull(filter.getPriceMax());
        assertNull(filter.getPage());
        assertNull(filter.getPageSize());
    }
}
