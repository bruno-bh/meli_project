package com.meli.productapi;

import com.fasterxml.jackson.databind.ObjectMapper;
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
@DisplayName("Testes de Integração da API de Produtos")
class ProductApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository repository;

    @BeforeEach
    void setUp() {
        // Limpar dados antes de cada teste
        repository.findAll().forEach(p -> repository.deleteById(p.getId()));
    }

    @Test
    @DisplayName("Deve criar, recuperar, atualizar e deletar um produto")
    void testFullProductLifecycle() throws Exception {
        // CREATE
        Map<String, Object> specifications = new HashMap<>();
        specifications.put("marca", "Samsung");
        specifications.put("voltagem", "110V");

        Product newProduct = Product.builder()
                .name("TV Samsung 55 polegadas")
                .description("TV LED Full HD")
                .price(1299.99)
                .size("55in")
                .weight(12.5)
                .color("Preto")
                .type("ELETRÔNICOS")
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
                .andExpect(jsonPath("$.price").value(1299.99));

        // UPDATE
        Map<String, Object> updatedSpecifications = new HashMap<>();
        updatedSpecifications.put("marca", "Samsung");
        updatedSpecifications.put("voltagem", "220V");
        updatedSpecifications.put("promocao", "Black Friday");

        Product updateData = Product.builder()
                .name("TV Samsung 55 polegadas - Smart TV")
                .description("TV LED Full HD com Smart TV")
                .price(999.99)
                .size("55in")
                .weight(12.5)
                .color("Preto")
                .type("ELETRÔNICOS")
                .rating(4.7)
                .specifications(updatedSpecifications)
                .build();

        mockMvc.perform(put("/api/v1/products/" + productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TV Samsung 55 polegadas - Smart TV"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.specifications.promocao").value("Black Friday"));

        // DELETE
        mockMvc.perform(delete("/api/v1/products/" + productId))
                .andExpect(status().isNoContent());

        // Verificar se foi deletado
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve criar múltiplos produtos com metadata diferente")
    void testCreateMultipleProductsWithDifferentMetadata() throws Exception {
        // Produto eletrônico
        Map<String, Object> electronicMetadata = new HashMap<>();
        electronicMetadata.put("marca", "Apple");
        electronicMetadata.put("voltagem", "110V");
        electronicMetadata.put("garantia_meses", 12);

        Product electronics = Product.builder()
                .name("iPhone 15 Pro")
                .description("Smartphone de última geração")
                .price(5999.99)
                .size("M")
                .weight(0.187)
                .color("Titânio")
                .type("ELETRÔNICOS")
                .rating(4.8)
                .specifications(electronicMetadata)
                .build();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(electronics)))
                .andExpect(status().isCreated());

        // Produto de vestuário
        Map<String, Object> clothMetadata = new HashMap<>();
        clothMetadata.put("tamanho_usa", "M");
        clothMetadata.put("tamanho_eu", "40");
        clothMetadata.put("material", "100% algodão");

        Product clothes = Product.builder()
                .name("Camiseta Premium")
                .description("Camiseta de alta qualidade")
                .price(129.90)
                .size("M")
                .weight(0.250)
                .color("Branco")
                .type("ROUPAS")
                .rating(4.5)
                .specifications(clothMetadata)
                .build();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clothes)))
                .andExpect(status().isCreated());

        // Produto de alimentos
        Map<String, Object> foodMetadata = new HashMap<>();
        foodMetadata.put("data_fabricacao", "2024-01-15");
        foodMetadata.put("data_vencimento", "2025-01-15");
        foodMetadata.put("nutriscore", "A");

        Product food = Product.builder()
                .name("Café Premium 500g")
                .description("Café torrado e moído")
                .price(45.90)
                .size("500g")
                .weight(0.5)
                .color("Marrom")
                .type("ALIMENTOS")
                .rating(4.7)
                .specifications(foodMetadata)
                .build();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(food)))
                .andExpect(status().isCreated());

        // Verificar total
        mockMvc.perform(get("/api/v1/products/stats/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));

        // Listar todos
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios na criação")
    void testValidationErrors() throws Exception {
        // Sem nome
        Product invalidProduct = Product.builder()
                .description("Descrição")
                .price(100.0)
                .type("ELETRÔNICOS")
                .build();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());

        // Com preço negativo
        invalidProduct = Product.builder()
                .name("Produto")
                .description("Descrição")
                .price(-50.0)
                .type("ELETRÔNICOS")
                .build();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar produto inexistente")
    void testGetNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound());
    }
}
