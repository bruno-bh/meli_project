package com.meli.productapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meli.productapi.model.Product;
import com.meli.productapi.model.ProductFilter;
import com.meli.productapi.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("Testes do ProductController")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id("123")
                .name("Produto Teste")
                .description("Descrição do teste")
                .price(99.99)
                .size("M")
                .weight(1.5)
                .color("Vermelho")
                .type("ELETRÔNICOS")
                .rating(4.3)
                .specifications(new HashMap<>(Map.of("marca", "Samsung")))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar lista de produtos sem filtros")
    void testGetAllProducts() throws Exception {
        List<Product> products = List.of(testProduct);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(products);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("123"))
                .andExpect(jsonPath("$[0].name").value("Produto Teste"));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Deve retornar produto por ID")
    void testGetProductById() throws Exception {
        when(productService.getProductById("123")).thenReturn(testProduct);

        mockMvc.perform(get("/api/v1/products/123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.name").value("Produto Teste"))
                .andExpect(jsonPath("$.price").value(99.99));

        verify(productService, times(1)).getProductById("123");
    }

    @Test
    @DisplayName("POST /api/v1/products - Deve criar novo produto")
    void testCreateProduct() throws Exception {
        Product newProduct = Product.builder()
                .name("Novo Produto")
                .description("Descrição nova")
                .price(150.0)
                .size("G")
                .weight(2.0)
                .color("Azul")
                .type("ROUPAS")
                .rating(4.0)
                .specifications(new HashMap<>(Map.of("tamanho", "XL")))
                .build();

        testProduct.setName("Novo Produto");
        testProduct.setPrice(150.0);

        when(productService.createProduct(any(Product.class))).thenReturn(testProduct);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Novo Produto"))
                .andExpect(jsonPath("$.price").value(150.0));

        verify(productService, times(1)).createProduct(any(Product.class));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Deve atualizar produto")
    void testUpdateProduct() throws Exception {
        Product updateData = Product.builder()
                .name("Produto Atualizado")
                .description("Descrição atualizada")
                .price(120.0)
                .size("M")
                .weight(1.8)
                .color("Preto")
                .type("ELETRÔNICOS")
                .rating(4.5)
                .specifications(new HashMap<>(Map.of("versao", "2024")))
                .build();

        testProduct.setName("Produto Atualizado");
        testProduct.setPrice(120.0);

        when(productService.updateProduct(eq("123"), any(Product.class)))
                .thenReturn(testProduct);

        mockMvc.perform(put("/api/v1/products/123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Produto Atualizado"))
                .andExpect(jsonPath("$.price").value(120.0));

        verify(productService, times(1)).updateProduct(eq("123"), any(Product.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - Deve deletar produto")
    void testDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct("123");

        mockMvc.perform(delete("/api/v1/products/123"))
                .andExpect(status().isNoContent());

        verify(productService, times(1)).deleteProduct("123");
    }

    @Test
    @DisplayName("GET /api/v1/products/stats/count - Deve retornar total de produtos")
    void testGetTotalProducts() throws Exception {
        when(productService.getTotalProducts()).thenReturn(5L);

        mockMvc.perform(get("/api/v1/products/stats/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(5));

        verify(productService, times(1)).getTotalProducts();
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve buscar produtos por nome e tipo")
    void testSearchProducts() throws Exception {
        Product product1 = Product.builder()
                .id("1")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(999.99)
                .description("Smartphone Apple")
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("iPhone 15")
                .type("CELLPHONES")
                .price(1099.99)
                .description("Smartphone Apple")
                .build();

        List<Product> searchResults = List.of(product1, product2);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(searchResults);

        mockMvc.perform(get("/api/v1/products")
                .param("name", "iPhone")
                .param("type", "CELLPHONES"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("iPhone 14"))
                .andExpect(jsonPath("$[1].name").value("iPhone 15"))
                .andExpect(jsonPath("$[0].type").value("CELLPHONES"));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve buscar apenas por nome")
    void testSearchProductsByNameOnly() throws Exception {
        Product product = Product.builder()
                .id("1")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(799.99)
                .description("Smartphone Samsung")
                .build();

        List<Product> searchResults = List.of(product);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(searchResults);

        mockMvc.perform(get("/api/v1/products")
                .param("name", "Samsung"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Samsung Galaxy"));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve buscar por descrição")
    void testSearchProductsByDescription() throws Exception {
        Product product = Product.builder()
                .id("1")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(999.99)
                .description("Smartphone Apple")
                .build();

        List<Product> searchResults = List.of(product);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(searchResults);

        mockMvc.perform(get("/api/v1/products")
                .param("description", "Apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Smartphone Apple"));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve buscar por faixa de preço")
    void testSearchProductsByPriceRange() throws Exception {
        Product product = Product.builder()
                .id("1")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(999.99)
                .build();

        List<Product> searchResults = List.of(product);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(searchResults);

        mockMvc.perform(get("/api/v1/products")
                .param("priceMin", "700.0")
                .param("priceMax", "1000.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price").value(999.99));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve buscar com paginação")
    void testSearchProductsWithPagination() throws Exception {
        List<Product> products = List.of(testProduct);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(products);

        mockMvc.perform(get("/api/v1/products")
                .param("page", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("123"));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("POST /api/v1/products - Deve retornar 400 quando name é null")
    void testCreateProductWithNullName() throws Exception {
        Product invalidProduct = Product.builder()
                .name(null)
                .price(99.99)
                .type("CELLPHONES")
                .build();

        when(productService.createProduct(any(Product.class)))
                .thenThrow(new IllegalArgumentException("Nome do produto é obrigatório"));

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());

        verify(productService, times(1)).createProduct(any(Product.class));
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar 400 quando pageSize é zero")
    void testSearchProductsWithZeroPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("page", "1")
                .param("pageSize", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar 400 quando pageSize é negativo")
    void testSearchProductsWithNegativePageSize() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("page", "1")
                .param("pageSize", "-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar 400 quando priceMin é zero")
    void testSearchProductsWithZeroPriceMin() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("priceMin", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar 400 quando priceMax é negativo")
    void testSearchProductsWithNegativePriceMax() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("priceMax", "-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar 400 quando priceMax < priceMin")
    void testSearchProductsWithMaxLessThanMin() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("priceMin", "1000")
                .param("priceMax", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar 400 quando page é string")
    void testSearchProductsWithInvalidPageType() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar 400 quando pageSize é string")
    void testSearchProductsWithInvalidPageSizeType() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("pageSize", "xyz"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar 400 quando priceMin é string")
    void testSearchProductsWithInvalidPriceMinType() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("priceMin", "invalido"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar 400 quando priceMax é string")
    void testSearchProductsWithInvalidPriceMaxType() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("priceMax", "invalido"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar 400 quando page é menor que 1")
    void testSearchProductsWithPageLessThanOne() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("page", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products - Deve retornar 400 quando pageSize é menor ou igual a 0")
    void testSearchProductsWithPageSizeZero() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("pageSize", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/products - Deve retornar 400 quando price é null")
    void testCreateProductWithNullPrice() throws Exception {
        Product invalidProduct = Product.builder()
                .name("Produto Teste")
                .price(null)
                .type("CELLPHONES")
                .build();

        when(productService.createProduct(any(Product.class)))
                .thenThrow(new IllegalArgumentException("Preço do produto deve ser um valor positivo"));

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());

        verify(productService, times(1)).createProduct(any(Product.class));
    }

    @Test
    @DisplayName("POST /api/v1/products - Deve retornar 400 quando type é null")
    void testCreateProductWithNullType() throws Exception {
        Product invalidProduct = Product.builder()
                .name("Produto Teste")
                .price(99.99)
                .type(null)
                .build();

        when(productService.createProduct(any(Product.class)))
                .thenThrow(new IllegalArgumentException("Tipo do produto é obrigatório"));

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());

        verify(productService, times(1)).createProduct(any(Product.class));
    }}