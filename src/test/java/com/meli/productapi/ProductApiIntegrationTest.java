package com.meli.productapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meli.productapi.model.MeasurableValue;
import com.meli.productapi.model.Product;
import com.meli.productapi.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Matchers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Product API — integration tests")
class ProductApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository repository;

    private static MeasurableValue price(Double value) {
        return MeasurableValue.builder().value(value).unit("BRL").build();
    }

    private static MeasurableValue size(Double value, String unit) {
        return MeasurableValue.builder().value(value).unit(unit).build();
    }

    private static MeasurableValue weight(Double value) {
        return MeasurableValue.builder().value(value).unit("kg").build();
    }

    @BeforeEach
    void setUp() {
        // Clear data before each test
        repository.findAll().forEach(p -> repository.deleteById(p.getId()));
    }

    @Test
    @DisplayName("Should create, retrieve, update, and delete a product")
    void testFullProductLifecycle() throws Exception {
        // CREATE
        Map<String, Object> specifications = new HashMap<>();
        specifications.put("brand", "Samsung");
        specifications.put("storage_gb", "128");

        Product newProduct = Product.builder()
                .name("TV Samsung 55 polegadas")
                .description("TV LED Full HD")
                .price(price(1299.99))
                .size(size(55.0, "in"))
                .weight(weight(12.5))
                .color("Preto")
                .type("CELLPHONES")
                .rating(4.5)
                .specifications(specifications)
                .build();

        String createResponse = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("TV Samsung 55 polegadas"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Product createdProduct = objectMapper.readValue(createResponse, Product.class);
        String productId = createdProduct.getId();

        // READ
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.name").value("TV Samsung 55 polegadas"))
                .andExpect(jsonPath("$.price.value").value(1299.99));

        // UPDATE
        Map<String, Object> updatedSpecifications = new HashMap<>();
        updatedSpecifications.put("brand", "Samsung");
        updatedSpecifications.put("storage_gb", "256");
        updatedSpecifications.put("memory_gb", "8");

        Product updateData = Product.builder()
                .name("TV Samsung 55 polegadas - Smart TV")
                .description("TV LED Full HD com Smart TV")
                .price(price(999.99))
                .size(size(55.0, "in"))
                .weight(weight(12.5))
                .color("Preto")
                .type("CELLPHONES")
                .rating(4.7)
                .specifications(updatedSpecifications)
                .build();

        mockMvc.perform(put("/api/v1/products/" + productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TV Samsung 55 polegadas - Smart TV"))
                .andExpect(jsonPath("$.price.value").value(999.99))
                .andExpect(jsonPath("$.specifications.memory_gb").value("8"));

        // DELETE
        mockMvc.perform(delete("/api/v1/products/" + productId))
                .andExpect(status().isNoContent());

        // Verify that the product was deleted
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should create multiple products with different metadata")
    void testCreateMultipleProductsWithDifferentMetadata() throws Exception {
        // Electronic product
        Map<String, Object> electronicMetadata = new HashMap<>();
        electronicMetadata.put("brand", "Apple");
        electronicMetadata.put("storage_gb", "256");
        electronicMetadata.put("memory_gb", "8");

        Product electronics = Product.builder()
                .name("iPhone 15 Pro")
                .description("Smartphone de última geração")
                .price(price(5999.99))
                .size(size(6.1, "inches"))
                .weight(weight(0.187))
                .color("Titânio")
                .type("CELLPHONES")
                .rating(4.8)
                .specifications(electronicMetadata)
                .build();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(electronics)))
                .andExpect(status().isCreated());

        // Clothing product
        Map<String, Object> clothMetadata = new HashMap<>();
        clothMetadata.put("size_us", "M");
        clothMetadata.put("size_eu", "40");
        clothMetadata.put("material", "100% algodão");

        Product clothes = Product.builder()
                .name("Camiseta Premium")
                .description("Camiseta de alta qualidade")
                .price(price(129.90))
                .size(size(null, "M"))
                .weight(weight(0.250))
                .color("Branco")
                .type("CLOTHING")
                .rating(4.5)
                .specifications(clothMetadata)
                .build();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clothes)))
                .andExpect(status().isCreated());

        // Food product
        Map<String, Object> foodMetadata = new HashMap<>();
        foodMetadata.put("manufacturing_date", "2024-01-15");
        foodMetadata.put("expiration_date", "2025-01-15");
        foodMetadata.put("nutriscore", "A");

        Product food = Product.builder()
                .name("Café Premium 500g")
                .description("Café torrado e moído")
                .price(price(45.90))
                .size(size(500.0, "g"))
                .weight(weight(0.5))
                .color("Marrom")
                .type("FOOD")
                .rating(4.7)
                .specifications(foodMetadata)
                .build();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(food)))
                .andExpect(status().isCreated());

        // Verify total count
        mockMvc.perform(get("/api/v1/products/stats/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));

        // List all products
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    @DisplayName("Should validate required fields on creation")
    void testValidationErrors() throws Exception {
        // Without name
        Product invalidProduct = Product.builder()
                .description("Descrição")
                .price(price(100.0))
                .type("CELLPHONES")
                .build();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());

        // With negative price
        invalidProduct = Product.builder()
                .name("Produto")
                .description("Descrição")
                .price(price(-50.0))
                .type("CELLPHONES")
                .build();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when fetching non-existent product")
    void testGetNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound());
    }

    // ── Comparison integration tests (task 11) ───────────────────────────

    @Test
    @DisplayName("Should compare 2 products of the same type E2E")
    void testCompareSameTypeProductsE2E() throws Exception {
        // Create two CELLPHONES products
        Product phone1 = Product.builder()
                .name("iPhone 15")
                .description("Apple flagship")
                .price(price(5999.99))
                .size(size(6.1, "inches"))
                .weight(weight(0.187))
                .type("CELLPHONES")
                .specifications(new HashMap<>(Map.of("brand", "Apple", "storage_gb", "256")))
                .build();

        Product phone2 = Product.builder()
                .name("Samsung Galaxy S24")
                .description("Samsung flagship")
                .price(price(4999.99))
                .size(size(6.2, "inches"))
                .weight(weight(0.167))
                .type("CELLPHONES")
                .specifications(new HashMap<>(Map.of("brand", "Samsung", "storage_gb", "128")))
                .build();

        String resp1 = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(phone1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id1 = objectMapper.readValue(resp1, Product.class).getId();

        String resp2 = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(phone2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id2 = objectMapper.readValue(resp2, Product.class).getId();

        // Compare
        mockMvc.perform(get("/api/v1/products/compare")
                .param("ids", id1 + "," + id2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productType").value("CELLPHONES"))
                .andExpect(jsonPath("$.products", Matchers.hasSize(2)));
    }

    @Test
    @DisplayName("Should return 409 when comparing products of different types E2E")
    void testCompareIncompatibleTypesE2E() throws Exception {
        Product phone = Product.builder()
                .name("Phone")
                .price(price(999.0))
                .type("CELLPHONES")
                .specifications(new HashMap<>(Map.of("brand", "Apple")))
                .build();

        Product shirt = Product.builder()
                .name("T-Shirt")
                .price(price(50.0))
                .type("CLOTHING")
                .specifications(new HashMap<>(Map.of("size_us", "M")))
                .build();

        String resp1 = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(phone)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id1 = objectMapper.readValue(resp1, Product.class).getId();

        String resp2 = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shirt)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id2 = objectMapper.readValue(resp2, Product.class).getId();

        mockMvc.perform(get("/api/v1/products/compare")
                .param("ids", id1 + "," + id2))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should compare products with specific filters E2E")
    void testCompareWithFiltersE2E() throws Exception {
        Product phone1 = Product.builder()
                .name("Phone A")
                .price(price(999.0))
                .type("CELLPHONES")
                .specifications(new HashMap<>(Map.of("brand", "Apple", "storage_gb", "256")))
                .build();

        Product phone2 = Product.builder()
                .name("Phone B")
                .price(price(799.0))
                .type("CELLPHONES")
                .specifications(new HashMap<>(Map.of("brand", "Samsung", "storage_gb", "128")))
                .build();

        String resp1 = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(phone1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id1 = objectMapper.readValue(resp1, Product.class).getId();

        String resp2 = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(phone2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id2 = objectMapper.readValue(resp2, Product.class).getId();

        mockMvc.perform(get("/api/v1/products/compare")
                .param("ids", id1 + "," + id2)
                .param("filters", "price,storage_gb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedFilters", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.products[0].price").exists())
                .andExpect(jsonPath("$.products[0].storage_gb").exists());
    }

    // ── Template integration tests (task 12) ─────────────────────────────

    @Test
    @DisplayName("GET /api/v1/templates — should return all templates E2E")
    void testGetAllTemplatesE2E() throws Exception {
        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CELLPHONES").exists())
                .andExpect(jsonPath("$.CLOTHING").exists());
    }

    @Test
    @DisplayName("GET /api/v1/templates/{type} — should return template for valid type E2E")
    void testGetTemplateByTypeE2E() throws Exception {
        mockMvc.perform(get("/api/v1/templates/CELLPHONES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CELLPHONES"))
                .andExpect(jsonPath("$.fields.price").exists());
    }

    @Test
    @DisplayName("GET /api/v1/templates/{type} — should return 404 for invalid type E2E")
    void testGetTemplateByTypeNotFoundE2E() throws Exception {
        mockMvc.perform(get("/api/v1/templates/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/templates/reload — should reload templates E2E")
    void testReloadTemplatesE2E() throws Exception {
        mockMvc.perform(post("/api/v1/templates/reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Templates reloaded successfully"))
                .andExpect(jsonPath("$.count").isNumber());
    }
}
