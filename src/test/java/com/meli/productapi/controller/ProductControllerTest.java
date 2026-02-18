package com.meli.productapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meli.productapi.model.MeasurableValue;
import com.meli.productapi.model.Product;
import com.meli.productapi.model.PageResponse;
import com.meli.productapi.model.ProductFilter;
import com.meli.productapi.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
@DisplayName("ProductController — REST endpoint tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct;

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
        testProduct = Product.builder()
                .id("123")
                .name("Produto Teste")
                .description("Descrição do teste")
                .price(price(99.99))
                .size(size(null, "M"))
                .weight(weight(1.5))
                .color("Vermelho")
                .type("CELLPHONES")
                .rating(4.3)
                .specifications(new HashMap<>(Map.of("marca", "Samsung")))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/products — should return product list without filters")
    void testGetAllProducts() throws Exception {
        List<Product> products = List.of(testProduct);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(PageResponse.ofAll(products));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value("123"))
                .andExpect(jsonPath("$.content[0].name").value("Produto Teste"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} — should return product by ID")
    void testGetProductById() throws Exception {
        when(productService.getProductById("123")).thenReturn(testProduct);

        mockMvc.perform(get("/api/v1/products/123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.name").value("Produto Teste"))
                .andExpect(jsonPath("$.price.value").value(99.99));

        verify(productService, times(1)).getProductById("123");
    }

    @Test
    @DisplayName("POST /api/v1/products — should create a new product")
    void testCreateProduct() throws Exception {
        Product newProduct = Product.builder()
                .name("Novo Produto")
                .description("Descrição nova")
                .price(price(150.0))
                .size(size(null, "G"))
                .weight(weight(2.0))
                .color("Azul")
                .type("CLOTHING")
                .rating(4.0)
                .specifications(new HashMap<>(Map.of("tamanho", "XL")))
                .build();

        testProduct.setName("Novo Produto");
        testProduct.setPrice(price(150.0));

        when(productService.createProduct(any(Product.class))).thenReturn(testProduct);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Novo Produto"))
                .andExpect(jsonPath("$.price.value").value(150.0));

        verify(productService, times(1)).createProduct(any(Product.class));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} — should update a product")
    void testUpdateProduct() throws Exception {
        Product updateData = Product.builder()
                .name("Produto Atualizado")
                .description("Descrição atualizada")
                .price(price(120.0))
                .size(size(null, "M"))
                .weight(weight(1.8))
                .color("Preto")
                .type("CELLPHONES")
                .rating(4.5)
                .specifications(new HashMap<>(Map.of("versao", "2024")))
                .build();

        testProduct.setName("Produto Atualizado");
        testProduct.setPrice(price(120.0));

        when(productService.updateProduct(eq("123"), any(Product.class)))
                .thenReturn(testProduct);

        mockMvc.perform(put("/api/v1/products/123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Produto Atualizado"))
                .andExpect(jsonPath("$.price.value").value(120.0));

        verify(productService, times(1)).updateProduct(eq("123"), any(Product.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} — should delete a product")
    void testDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct("123");

        mockMvc.perform(delete("/api/v1/products/123"))
                .andExpect(status().isNoContent());

        verify(productService, times(1)).deleteProduct("123");
    }

    @Test
    @DisplayName("GET /api/v1/products/stats/count — should return total product count")
    void testGetTotalProducts() throws Exception {
        when(productService.getTotalProducts()).thenReturn(5L);

        mockMvc.perform(get("/api/v1/products/stats/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(5));

        verify(productService, times(1)).getTotalProducts();
    }

    @Test
    @DisplayName("GET /api/v1/products — should search products by name and type")
    void testSearchProducts() throws Exception {
        Product product1 = Product.builder()
                .id("1")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(price(999.99))
                .description("Smartphone Apple")
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("iPhone 15")
                .type("CELLPHONES")
                .price(price(1099.99))
                .description("Smartphone Apple")
                .build();

        List<Product> searchResults = List.of(product1, product2);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(PageResponse.ofAll(searchResults));

        mockMvc.perform(get("/api/v1/products")
                .param("name", "iPhone")
                .param("type", "CELLPHONES"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].name").value("iPhone 14"))
                .andExpect(jsonPath("$.content[1].name").value("iPhone 15"))
                .andExpect(jsonPath("$.content[0].type").value("CELLPHONES"));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products — should search products by name only")
    void testSearchProductsByNameOnly() throws Exception {
        Product product = Product.builder()
                .id("1")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(price(799.99))
                .description("Smartphone Samsung")
                .build();

        List<Product> searchResults = List.of(product);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(PageResponse.ofAll(searchResults));

        mockMvc.perform(get("/api/v1/products")
                .param("name", "Samsung"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Samsung Galaxy"));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products — should filter products by specification")
    void testSearchProductsBySpecification() throws Exception {
        Product product = Product.builder()
                .id("1")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(price(799.99))
                .build();

        List<Product> searchResults = List.of(product);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(PageResponse.ofAll(searchResults));

        mockMvc.perform(get("/api/v1/products")
                .param("type", "CELLPHONES")
                .param("brand", "Samsung"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Samsung Galaxy"));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products — should return 400 when type is invalid")
    void testSearchProductsWithInvalidType() throws Exception {
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenThrow(new IllegalArgumentException(
                        "Invalid product type: 'XPTO'. Valid types: [CELLPHONES, COMPUTERS, CLOTHING]"));

        mockMvc.perform(get("/api/v1/products")
                .param("type", "XPTO"))
                .andExpect(status().isBadRequest());

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products — should return 400 when spec key is invalid for type")
    void testSearchProductsWithInvalidSpecKey() throws Exception {
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenThrow(new IllegalArgumentException(
                        "Invalid specification key 'foo' for product type 'CELLPHONES'."));

        mockMvc.perform(get("/api/v1/products")
                .param("type", "CELLPHONES")
                .param("foo", "bar"))
                .andExpect(status().isBadRequest());

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products — should search products by price range")
    void testSearchProductsByPriceRange() throws Exception {
        Product product = Product.builder()
                .id("1")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(price(999.99))
                .build();

        List<Product> searchResults = List.of(product);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(PageResponse.ofAll(searchResults));

        mockMvc.perform(get("/api/v1/products")
                .param("priceMin", "700.0")
                .param("priceMax", "1000.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price.value").value(999.99));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("GET /api/v1/products — should search products with pagination")
    void testSearchProductsWithPagination() throws Exception {
        List<Product> products = List.of(testProduct);
        when(productService.searchProducts(any(ProductFilter.class)))
                .thenReturn(PageResponse.of(products, 1, 10));

        mockMvc.perform(get("/api/v1/products")
                .param("page", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("123"));

        verify(productService, times(1))
                .searchProducts(any(ProductFilter.class));
    }

    @Test
    @DisplayName("POST /api/v1/products — should return 400 when name is null")
    void testCreateProductWithNullName() throws Exception {
        Product invalidProduct = Product.builder()
                .name(null)
                .price(price(99.99))
                .type("CELLPHONES")
                .build();

        when(productService.createProduct(any(Product.class)))
                .thenThrow(new IllegalArgumentException("Product name is required"));

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());

        verify(productService, times(1)).createProduct(any(Product.class));
    }

    @Test
    @DisplayName("GET /api/v1/products — should return 400 when pageSize is zero")
    void testSearchProductsWithZeroPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("page", "1")
                .param("pageSize", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products — should return 400 when pageSize is negative")
    void testSearchProductsWithNegativePageSize() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("page", "1")
                .param("pageSize", "-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products — should return 400 when priceMin is zero")
    void testSearchProductsWithZeroPriceMin() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("priceMin", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products — should return 400 when priceMax is negative")
    void testSearchProductsWithNegativePriceMax() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("priceMax", "-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products — should return 400 when priceMax < priceMin")
    void testSearchProductsWithMaxLessThanMin() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("priceMin", "1000")
                .param("priceMax", "500"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "GET /api/v1/products — should return 400 when {0} is a non-numeric string: ''{1}''")
    @CsvSource({
            "page, abc",
            "pageSize, xyz",
            "priceMin, invalido",
            "priceMax, invalido"
    })
    @DisplayName("GET /api/v1/products — should return 400 for non-numeric query params")
    void testSearchProductsWithInvalidParamType(String paramName, String paramValue) throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param(paramName, paramValue))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products — should return 400 when page is less than 1")
    void testSearchProductsWithPageLessThanOne() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("page", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/products — should return 400 when price is null")
    void testCreateProductWithNullPrice() throws Exception {
        Product invalidProduct = Product.builder()
                .name("Produto Teste")
                .price(null)
                .type("CELLPHONES")
                .build();

        when(productService.createProduct(any(Product.class)))
                .thenThrow(new IllegalArgumentException("Product price must be a positive value greater than zero"));

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());

        verify(productService, times(1)).createProduct(any(Product.class));
    }

    @Test
    @DisplayName("POST /api/v1/products — should return 400 when type is null")
    void testCreateProductWithNullType() throws Exception {
        Product invalidProduct = Product.builder()
                .name("Produto Teste")
                .price(price(99.99))
                .type(null)
                .build();

        when(productService.createProduct(any(Product.class)))
                .thenThrow(new IllegalArgumentException("Product type is required"));

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isBadRequest());

        verify(productService, times(1)).createProduct(any(Product.class));
    }
}
