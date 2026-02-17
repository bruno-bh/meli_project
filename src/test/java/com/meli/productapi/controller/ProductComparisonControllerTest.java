package com.meli.productapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meli.productapi.model.Product;
import com.meli.productapi.model.ProductType;
import com.meli.productapi.model.ProductComparisonResponse;
import com.meli.productapi.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        reset(productService);
    }

    @Test
    void testCompareProductsAllFields() throws Exception {
        // Arrange
        List<Map<String, Object>> comparisonProducts = new ArrayList<>();
        Map<String, Object> product1 = new LinkedHashMap<>();
        product1.put("id", "1");
        product1.put("name", "iPhone 15 Pro");
        product1.put("price", 5999.99);
        comparisonProducts.add(product1);

        Map<String, Object> product2 = new LinkedHashMap<>();
        product2.put("id", "2");
        product2.put("name", "iPhone 14 Pro");
        product2.put("price", 4999.99);
        comparisonProducts.add(product2);

        ProductComparisonResponse response = ProductComparisonResponse.builder()
                .productType("CELLPHONES")
                .appliedFilters(new ArrayList<>())
                .products(comparisonProducts)
                .build();

        when(productService.compareProducts(anyList(), any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/compare")
                .param("ids", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productType", equalTo("CELLPHONES")))
                .andExpect(jsonPath("$.products", hasSize(2)))
                .andExpect(jsonPath("$.products[0].name", equalTo("iPhone 15 Pro")))
                .andExpect(jsonPath("$.products[1].name", equalTo("iPhone 14 Pro")));

        verify(productService, times(1)).compareProducts(anyList(), any());
    }

    @Test
    void testCompareProductsWithFilters() throws Exception {
        // Arrange
        List<Map<String, Object>> comparisonProducts = new ArrayList<>();
        Map<String, Object> product1 = new LinkedHashMap<>();
        product1.put("name", "iPhone 15 Pro");
        product1.put("price", 5999.99);
        comparisonProducts.add(product1);

        Map<String, Object> product2 = new LinkedHashMap<>();
        product2.put("name", "iPhone 14 Pro");
        product2.put("price", 4999.99);
        comparisonProducts.add(product2);

        ProductComparisonResponse response = ProductComparisonResponse.builder()
                .productType("CELLPHONES")
                .appliedFilters(Arrays.asList("name", "price"))
                .products(comparisonProducts)
                .build();

        when(productService.compareProducts(anyList(), anyList())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/compare")
                .param("ids", "1,2")
                .param("filters", "name,price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedFilters", hasSize(2)))
                .andExpect(jsonPath("$.products", hasSize(2)));

        verify(productService, times(1)).compareProducts(anyList(), anyList());
    }

    @Test
    void testCompareProductsIncompatibleTypes() throws Exception {
        // Arrange
        when(productService.compareProducts(anyList(), any()))
                .thenThrow(new com.meli.productapi.exception.IncompatibleProductTypesException("Products must have the same type"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/compare")
                .param("ids", "1,2"))
                .andExpect(status().isConflict());

        verify(productService, times(1)).compareProducts(anyList(), any());
    }

    @Test
    void testCompareProductsEmptyIds() throws Exception {
        // Arrange
        when(productService.compareProducts(anyList(), any()))
                .thenThrow(new IllegalArgumentException("At least one product ID is required"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/compare")
                .param("ids", ""))
                .andExpect(status().isBadRequest());

        verify(productService, times(1)).compareProducts(anyList(), any());
    }

    @Test
    void testCompareThreeProducts() throws Exception {
        // Arrange
        List<Map<String, Object>> comparisonProducts = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> product = new LinkedHashMap<>();
            product.put("id", String.valueOf(i));
            product.put("name", "iPhone 15 Pro " + i);
            product.put("price", 5999.99 + i * 100);
            comparisonProducts.add(product);
        }

        ProductComparisonResponse response = ProductComparisonResponse.builder()
                .productType("CELLPHONES")
                .appliedFilters(new ArrayList<>())
                .products(comparisonProducts)
                .build();

        when(productService.compareProducts(anyList(), any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/products/compare")
                .param("ids", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(3)))
                .andExpect(jsonPath("$.products.length()", equalTo(3)));

        verify(productService, times(1)).compareProducts(anyList(), any());
    }
}
