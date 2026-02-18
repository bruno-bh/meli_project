package com.meli.productapi.service;

import com.meli.productapi.exception.ProductNotFoundException;
import com.meli.productapi.model.MeasurableValue;
import com.meli.productapi.model.Product;
import com.meli.productapi.model.ProductFilter;
import com.meli.productapi.model.template.FieldDefinition;
import com.meli.productapi.model.template.ProductTemplate;
import com.meli.productapi.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService — business logic tests")
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private ProductTemplateService templateService;

    private ProductService productService;

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
        productService = new ProductService(repository, templateService);

        testProduct = Product.builder()
                .id("123")
                .name("Teste Produto")
                .description("Descrição do teste")
                .price(price(99.99))
                .size(size(null, "M"))
                .weight(weight(1.5))
                .color("Vermelho")
                .type("CELLPHONES")
                .rating(4.5)
                .specifications(new HashMap<>(Map.of("marca", "Samsung", "voltagem", "110V")))
                .build();
    }

    private void mockValidType(String type) {
        lenient().when(templateService.isValidType(type)).thenReturn(true);
        lenient().when(templateService.getTemplate(type)).thenReturn(Optional.of(
                ProductTemplate.builder()
                        .name(type)
                        .displayName(type)
                        .fields(Map.of(
                                "price", FieldDefinition.builder().type("number").defaultUnit("BRL").required(true).comparable(true).build(),
                                "size", FieldDefinition.builder().type("number").defaultUnit("inches").required(false).comparable(true).build(),
                                "weight", FieldDefinition.builder().type("number").defaultUnit("kg").required(false).comparable(true).build()
                        ))
                        .specifications(Map.of())
                        .build()
        ));
    }

    private void mockValidTypeWithSpecs(String type, Map<String, FieldDefinition> specs) {
        lenient().when(templateService.isValidType(type)).thenReturn(true);
        lenient().when(templateService.getTemplate(type)).thenReturn(Optional.of(
                ProductTemplate.builder()
                        .name(type)
                        .displayName(type)
                        .fields(Map.of(
                                "price", FieldDefinition.builder().type("number").defaultUnit("BRL").required(true).comparable(true).build(),
                                "size", FieldDefinition.builder().type("number").defaultUnit("inches").required(false).comparable(true).build(),
                                "weight", FieldDefinition.builder().type("number").defaultUnit("kg").required(false).comparable(true).build()
                        ))
                        .specifications(specs)
                        .build()
        ));
    }

    private void mockValidTypeWithRequiredSize(String type) {
        lenient().when(templateService.isValidType(type)).thenReturn(true);
        lenient().when(templateService.getTemplate(type)).thenReturn(Optional.of(
                ProductTemplate.builder()
                        .name(type)
                        .displayName(type)
                        .fields(Map.of(
                                "price", FieldDefinition.builder().type("number").defaultUnit("BRL").required(true).comparable(true).build(),
                                "size", FieldDefinition.builder().type("number").defaultUnit("inches").required(true).comparable(true).build(),
                                "weight", FieldDefinition.builder().type("number").defaultUnit("kg").required(false).comparable(true).build()
                        ))
                        .specifications(Map.of(
                                "brand", FieldDefinition.builder().type("text").required(true).comparable(false).build()
                        ))
                        .build()
        ));
    }

    @Test
    @DisplayName("Should list all products without filters")
    void testGetAllProducts() {
        List<Product> products = List.of(testProduct);
        when(repository.findAll()).thenReturn(products);

        ProductFilter filter = ProductFilter.builder().build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(1, result.size());
        assertEquals(testProduct.getName(), result.get(0).getName());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return product by ID")
    void testGetProductById() {
        when(repository.findById("123")).thenReturn(Optional.of(testProduct));

        Product result = productService.getProductById("123");

        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("Teste Produto", result.getName());
        verify(repository, times(1)).findById("123");
    }

    @Test
    @DisplayName("Should throw exception when product is not found")
    void testGetProductByIdNotFound() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> {
            productService.getProductById("999");
        });

        verify(repository, times(1)).findById("999");
    }

    @Test
    @DisplayName("Should create a new product")
    void testCreateProduct() {
        mockValidTypeWithSpecs("CLOTHING", Map.of(
                "tamanho_usa", FieldDefinition.builder().type("text").required(false).comparable(false).build()
        ));

        Product newProduct = Product.builder()
                .name("Novo Produto")
                .description("Descrição nova")
                .price(price(50.0))
                .size(size(null, "G"))
                .weight(weight(2.0))
                .color("Azul")
                .type("CLOTHING")
                .rating(3.8)
                .specifications(new HashMap<>(Map.of("tamanho_usa", "XL")))
                .build();

        when(repository.save(any(Product.class))).thenReturn(newProduct);

        Product result = productService.createProduct(newProduct);

        assertNotNull(result);
        assertEquals("Novo Produto", result.getName());
        verify(repository, times(1)).save(any(Product.class));
    }

    @ParameterizedTest(name = "Should reject product with invalid name: ''{0}''")
    @NullAndEmptySource
    @DisplayName("Should throw exception when creating product with null or empty name")
    void testCreateProductWithInvalidName(String invalidName) {
        Product invalidProduct = Product.builder()
                .name(invalidName)
                .price(price(50.0))
                .type("CELLPHONES")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(invalidProduct);
        });

        assertTrue(exception.getMessage().contains("Product name is required"));
    }

    static Stream<MeasurableValue> invalidPriceProvider() {
        return Stream.of(
                null,
                MeasurableValue.builder().value(null).unit("BRL").build(),
                MeasurableValue.builder().value(-10.0).unit("BRL").build(),
                MeasurableValue.builder().value(0.0).unit("BRL").build()
        );
    }

    @ParameterizedTest(name = "Should reject product with invalid price: {0}")
    @MethodSource("invalidPriceProvider")
    @DisplayName("Should throw exception when creating product with null, zero, or negative price")
    void testCreateProductWithInvalidPrice(MeasurableValue invalidPrice) {
        Product invalidProduct = Product.builder()
                .name("Produto Teste")
                .price(invalidPrice)
                .type("CELLPHONES")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(invalidProduct);
        });

        assertTrue(exception.getMessage().contains("Product price"));
    }

    @ParameterizedTest(name = "Should reject product with invalid type: ''{0}''")
    @NullAndEmptySource
    @DisplayName("Should throw exception when creating product with null or empty type")
    void testCreateProductWithInvalidType(String invalidType) {
        Product invalidProduct = Product.builder()
                .name("Produto Teste")
                .price(price(50.0))
                .type(invalidType)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(invalidProduct);
        });

        assertTrue(exception.getMessage().contains("Product type is required"));
    }

    @Test
    @DisplayName("Should update an existing product")
    void testUpdateProduct() {
        mockValidTypeWithSpecs("CELLPHONES", Map.of(
                "modelo", FieldDefinition.builder().type("text").required(false).comparable(false).build()
        ));

        Product updateData = Product.builder()
                .name("Produto Atualizado")
                .description("Descrição atualizada")
                .price(price(120.0))
                .size(size(null, "G"))
                .weight(weight(2.0))
                .color("Verde")
                .type("CELLPHONES")
                .rating(4.2)
                .specifications(new HashMap<>(Map.of("modelo", "2024")))
                .build();

        Product updatedProduct = Product.builder()
                .id("123")
                .name("Produto Atualizado")
                .description("Descrição atualizada")
                .price(price(120.0))
                .size(size(null, "G"))
                .weight(weight(2.0))
                .color("Verde")
                .type("CELLPHONES")
                .rating(4.2)
                .specifications(new HashMap<>(Map.of("modelo", "2024")))
                .build();

        when(repository.findById("123")).thenReturn(Optional.of(testProduct));
        when(repository.save(any(Product.class))).thenReturn(updatedProduct);

        Product result = productService.updateProduct("123", updateData);

        assertNotNull(result);
        assertEquals("Produto Atualizado", result.getName());
        assertEquals(120.0, result.getPrice().getValue());
        verify(repository, times(1)).findById("123");
        verify(repository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when updating product with null name")
    void testUpdateProductWithNullName() {
        when(repository.findById("123")).thenReturn(Optional.of(testProduct));

        Product updateData = Product.builder()
                .name(null)
                .price(price(100.0))
                .type("CELLPHONES")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                productService.updateProduct("123", updateData));

        assertTrue(exception.getMessage().contains("Product name is required"));
    }

    @Test
    @DisplayName("Should throw exception when updating product with negative price")
    void testUpdateProductWithNegativePrice() {
        when(repository.findById("123")).thenReturn(Optional.of(testProduct));

        Product updateData = Product.builder()
                .name("Valid Name")
                .price(price(-50.0))
                .type("CELLPHONES")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                productService.updateProduct("123", updateData));

        assertTrue(exception.getMessage().contains("Product price"));
    }

    @Test
    @DisplayName("Should throw exception when updating product with invalid type")
    void testUpdateProductWithInvalidType() {
        when(repository.findById("123")).thenReturn(Optional.of(testProduct));

        Product updateData = Product.builder()
                .name("Valid Name")
                .price(price(100.0))
                .type("NONEXISTENT")
                .build();

        when(templateService.isValidType("NONEXISTENT")).thenReturn(false);
        when(templateService.getValidTypeNames()).thenReturn(java.util.List.of("CELLPHONES", "CLOTHING"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                productService.updateProduct("123", updateData));

        assertTrue(exception.getMessage().contains("Invalid product type"));
    }

    @Test
    @DisplayName("Should delete a product")
    void testDeleteProduct() {
        Product product = Product.builder()
                .id("123")
                .name("Produto Teste")
                .description("Descrição")
                .price(price(99.99))
                .build();

        when(repository.findById("123")).thenReturn(Optional.of(product));

        productService.deleteProduct("123");

        verify(repository, times(1)).findById("123");
        verify(repository, times(1)).deleteById("123");
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent product")
    void testDeleteProductNotFound() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> {
            productService.deleteProduct("999");
        });

        verify(repository, times(1)).findById("999");
    }

    @Test
    @DisplayName("Should return total product count")
    void testGetTotalProducts() {
        when(repository.count()).thenReturn(5L);

        long result = productService.getTotalProducts();

        assertEquals(5L, result);
        verify(repository, times(1)).count();
    }

    @Test
    @DisplayName("Should search products by name and type")
    void testSearchProductsByNameAndType() {
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

        Product product3 = Product.builder()
                .id("3")
                .name("Samsung TV")
                .type("TV")
                .price(price(599.99))
                .description("Smart TV")
                .build();

        List<Product> allProducts = List.of(product1, product2, product3);
        when(repository.findAll()).thenReturn(allProducts);

        when(templateService.isValidType("CELLPHONES")).thenReturn(true);

        ProductFilter filter = ProductFilter.builder()
                .name("iPhone")
                .type("CELLPHONES")
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getType().equals("CELLPHONES")));
        assertTrue(result.stream().allMatch(p -> p.getName().contains("iPhone")));
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should search products by name only (type null)")
    void testSearchProductsByNameOnly() {
        Product product1 = Product.builder()
                .id("1")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(price(799.99))
                .description("Smartphone Samsung")
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("Samsung TV")
                .type("TV")
                .price(price(599.99))
                .description("Smart TV")
                .build();

        List<Product> allProducts = List.of(product1, product2);
        when(repository.findAll()).thenReturn(allProducts);

        ProductFilter filter = ProductFilter.builder()
                .name("Samsung")
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getName().contains("Samsung")));
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should search products by type only (name null)")
    void testSearchProductsByTypeOnly() {
        Product product1 = Product.builder()
                .id("1")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(price(999.99))
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(price(799.99))
                .build();

        List<Product> allProducts = List.of(product1, product2);
        when(repository.findAll()).thenReturn(allProducts);
        when(templateService.isValidType("CELLPHONES")).thenReturn(true);

        ProductFilter filter = ProductFilter.builder()
                .type("CELLPHONES")
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getType().equals("CELLPHONES")));
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should throw exception when searching with invalid type")
    void testSearchProductsWithInvalidType() {
        when(templateService.isValidType("XPTO")).thenReturn(false);
        when(templateService.getValidTypeNames()).thenReturn(List.of("CELLPHONES", "COMPUTERS", "CLOTHING"));

        ProductFilter filter = ProductFilter.builder()
                .type("XPTO")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                productService.searchProducts(filter));

        assertTrue(exception.getMessage().contains("Invalid product type"));
        assertTrue(exception.getMessage().contains("XPTO"));
        assertTrue(exception.getMessage().contains("CELLPHONES"));
    }

    @Test
    @DisplayName("Should filter products by specification with type informed")
    void testSearchProductsBySpecificationWithType() {
        Product product1 = Product.builder()
                .id("1")
                .name("iPhone 15")
                .type("CELLPHONES")
                .price(price(999.99))
                .specifications(new HashMap<>(Map.of("brand", "Apple", "storage_gb", "256")))
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(price(799.99))
                .specifications(new HashMap<>(Map.of("brand", "Samsung", "storage_gb", "128")))
                .build();

        List<Product> allProducts = List.of(product1, product2);
        when(repository.findAll()).thenReturn(allProducts);
        when(templateService.isValidType("CELLPHONES")).thenReturn(true);
        when(templateService.getMetadataFields("CELLPHONES")).thenReturn(List.of("brand", "storage_gb", "memory_gb"));

        ProductFilter filter = ProductFilter.builder()
                .type("CELLPHONES")
                .specifications(Map.of("brand", "Samsung"))
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(1, result.size());
        assertEquals("Samsung Galaxy", result.get(0).getName());
    }

    @Test
    @DisplayName("Should filter products by specification without type informed")
    void testSearchProductsBySpecificationWithoutType() {
        Product product1 = Product.builder()
                .id("1")
                .name("iPhone 15")
                .type("CELLPHONES")
                .price(price(999.99))
                .specifications(new HashMap<>(Map.of("brand", "Apple")))
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(price(799.99))
                .specifications(new HashMap<>(Map.of("brand", "Samsung")))
                .build();

        List<Product> allProducts = List.of(product1, product2);
        when(repository.findAll()).thenReturn(allProducts);
        when(templateService.getAllSpecificationKeys()).thenReturn(Set.of("brand", "storage_gb", "memory_gb"));

        ProductFilter filter = ProductFilter.builder()
                .specifications(Map.of("brand", "Apple"))
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(1, result.size());
        assertEquals("iPhone 15", result.get(0).getName());
    }

    @Test
    @DisplayName("Should combine specification filter with other filters")
    void testSearchProductsBySpecCombinedWithOtherFilters() {
        Product product1 = Product.builder()
                .id("1")
                .name("iPhone 15")
                .type("CELLPHONES")
                .price(price(999.99))
                .specifications(new HashMap<>(Map.of("brand", "Apple")))
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(price(699.99))
                .specifications(new HashMap<>(Map.of("brand", "Apple")))
                .build();

        Product product3 = Product.builder()
                .id("3")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(price(899.99))
                .specifications(new HashMap<>(Map.of("brand", "Samsung")))
                .build();

        List<Product> allProducts = List.of(product1, product2, product3);
        when(repository.findAll()).thenReturn(allProducts);
        when(templateService.isValidType("CELLPHONES")).thenReturn(true);
        when(templateService.getMetadataFields("CELLPHONES")).thenReturn(List.of("brand", "storage_gb"));

        // Filter by type + spec + price range
        ProductFilter filter = ProductFilter.builder()
                .type("CELLPHONES")
                .specifications(Map.of("brand", "Apple"))
                .priceMin(800.0)
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(1, result.size());
        assertEquals("iPhone 15", result.get(0).getName());
    }

    @Test
    @DisplayName("Should throw exception for invalid spec key with type informed")
    void testSearchProductsWithInvalidSpecKeyWithType() {
        when(templateService.isValidType("CELLPHONES")).thenReturn(true);
        when(templateService.getMetadataFields("CELLPHONES")).thenReturn(List.of("brand", "storage_gb", "memory_gb"));

        ProductFilter filter = ProductFilter.builder()
                .type("CELLPHONES")
                .specifications(Map.of("foo", "bar"))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                productService.searchProducts(filter));

        assertTrue(exception.getMessage().contains("Invalid specification key 'foo'"));
        assertTrue(exception.getMessage().contains("CELLPHONES"));
    }

    @Test
    @DisplayName("Should throw exception for invalid spec key without type informed")
    void testSearchProductsWithInvalidSpecKeyWithoutType() {
        when(templateService.getAllSpecificationKeys()).thenReturn(Set.of("brand", "storage_gb", "memory_gb"));

        ProductFilter filter = ProductFilter.builder()
                .specifications(Map.of("nonexistent_key", "value"))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                productService.searchProducts(filter));

        assertTrue(exception.getMessage().contains("Invalid specification key 'nonexistent_key'"));
        assertTrue(exception.getMessage().contains("does not exist in any product type template"));
    }

    @Test
    @DisplayName("Should return empty list when no products match")
    void testSearchProductsNoResults() {
        Product product = Product.builder()
                .id("1")
                .name("iPad")
                .type("TABLETS")
                .price(price(599.99))
                .build();

        List<Product> allProducts = List.of(product);
        when(repository.findAll()).thenReturn(allProducts);
        when(templateService.isValidType("CELLPHONES")).thenReturn(true);

        ProductFilter filter = ProductFilter.builder()
                .name("iPhone")
                .type("CELLPHONES")
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(0, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should search products by price range")
    void testSearchProductsByPriceRange() {
        Product product1 = Product.builder()
                .id("1")
                .name("iPhone 14")
                .type("CELLPHONES")
                .price(price(999.99))
                .build();

        Product product2 = Product.builder()
                .id("2")
                .name("Samsung Galaxy")
                .type("CELLPHONES")
                .price(price(799.99))
                .build();

        Product product3 = Product.builder()
                .id("3")
                .name("Nokia")
                .type("CELLPHONES")
                .price(price(299.99))
                .build();

        List<Product> allProducts = List.of(product1, product2, product3);
        when(repository.findAll()).thenReturn(allProducts);

        ProductFilter filter = ProductFilter.builder()
                .priceMin(700.0)
                .priceMax(1000.0)
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p -> p.getPrice().getValue() >= 700.0 && p.getPrice().getValue() <= 1000.0));
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should apply pagination correctly")
    void testSearchProductsWithPagination() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(price(100.0)).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(price(200.0)).build(),
                Product.builder().id("3").name("P3").type("CELLPHONES").price(price(300.0)).build(),
                Product.builder().id("4").name("P4").type("CELLPHONES").price(price(400.0)).build(),
                Product.builder().id("5").name("P5").type("CELLPHONES").price(price(500.0)).build()
        );

        when(repository.findAll()).thenReturn(products);

        // Page 1, size 2
        ProductFilter filter1 = ProductFilter.builder()
                .page(1)
                .pageSize(2)
                .build();
        List<Product> page1 = productService.searchProducts(filter1);
        assertEquals(2, page1.size());
        assertEquals("P1", page1.get(0).getName());
        assertEquals("P2", page1.get(1).getName());

        // Page 2, size 2
        ProductFilter filter2 = ProductFilter.builder()
                .page(2)
                .pageSize(2)
                .build();
        List<Product> page2 = productService.searchProducts(filter2);
        assertEquals(2, page2.size());
        assertEquals("P3", page2.get(0).getName());
        assertEquals("P4", page2.get(1).getName());

        // Page 3, size 2
        ProductFilter filter3 = ProductFilter.builder()
                .page(3)
                .pageSize(2)
                .build();
        List<Product> page3 = productService.searchProducts(filter3);
        assertEquals(1, page3.size());
        assertEquals("P5", page3.get(0).getName());
    }

    @Test
    @DisplayName("Should return all products when pageSize is null")
    void testSearchProductsWithoutPagination() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(price(100.0)).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(price(200.0)).build(),
                Product.builder().id("3").name("P3").type("CELLPHONES").price(price(300.0)).build()
        );

        when(repository.findAll()).thenReturn(products);

        ProductFilter filter = ProductFilter.builder().build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(3, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when page is out of bounds")
    void testSearchProductsPageOutOfBounds() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(price(100.0)).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(price(200.0)).build()
        );

        when(repository.findAll()).thenReturn(products);

        ProductFilter filter = ProductFilter.builder()
                .page(10)
                .pageSize(2)
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(0, result.size());
        verify(repository, times(1)).findAll();
    }

    /**
     * Intentional: validates that the service layer does NOT validate priceMin bounds.
     * Bound validation (>= 1) is enforced at the controller layer via @Min on ProductFilter.
     * This test documents the deliberate separation of concerns.
     */
    @Test
    @DisplayName("Should accept priceMin zero/negative at service layer (validation is at controller via @Min)")
    void testSearchProductsWithInvalidPriceMin() {
        when(repository.findAll()).thenReturn(List.of());

        ProductFilter filter1 = ProductFilter.builder()
                .priceMin(0.0)
                .build();
        assertDoesNotThrow(() -> productService.searchProducts(filter1));

        ProductFilter filter2 = ProductFilter.builder()
                .priceMin(-10.0)
                .build();
        assertDoesNotThrow(() -> productService.searchProducts(filter2));
    }

    /**
     * Intentional: validates that the service layer does NOT validate priceMax bounds.
     * Bound validation (>= 1) is enforced at the controller layer via @Min on ProductFilter.
     * This test documents the deliberate separation of concerns.
     */
    @Test
    @DisplayName("Should accept priceMax zero/negative at service layer (validation is at controller via @Min)")
    void testSearchProductsWithInvalidPriceMax() {
        when(repository.findAll()).thenReturn(List.of());

        ProductFilter filter1 = ProductFilter.builder()
                .priceMax(0.0)
                .build();
        assertDoesNotThrow(() -> productService.searchProducts(filter1));

        ProductFilter filter2 = ProductFilter.builder()
                .priceMax(-5.0)
                .build();
        assertDoesNotThrow(() -> productService.searchProducts(filter2));
    }

    @Test
    @DisplayName("Should throw exception when priceMax is less than priceMin")
    void testSearchProductsWithMaxLessThanMin() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ProductFilter filter = ProductFilter.builder()
                    .priceMin(1000.0)
                    .priceMax(500.0)
                    .build();
            productService.searchProducts(filter);
        });

        assertTrue(exception.getMessage().contains("Maximum price cannot be less than the minimum price"));
    }

    @Test
    @DisplayName("Should accept valid priceMin and priceMax")
    void testSearchProductsWithValidPriceRange() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(price(100.0)).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(price(500.0)).build()
        );

        when(repository.findAll()).thenReturn(products);

        ProductFilter filter = ProductFilter.builder()
                .priceMin(50.0)
                .priceMax(1000.0)
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @ParameterizedTest(name = "Should throw exception when pageSize is {0}")
    @ValueSource(ints = {0, -5})
    @DisplayName("Should throw exception when pageSize is zero or negative")
    void testSearchProductsWithInvalidPageSize(int invalidPageSize) {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(price(100.0)).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(price(200.0)).build()
        );

        when(repository.findAll()).thenReturn(products);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ProductFilter filter = ProductFilter.builder()
                    .page(1)
                    .pageSize(invalidPageSize)
                    .build();
            productService.searchProducts(filter);
        });

        assertTrue(exception.getMessage().contains("Page size must be greater than zero"));
    }

    @Test
    @DisplayName("Should treat null page as page 1")
    void testSearchProductsWithNullPage() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(price(100.0)).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(price(200.0)).build(),
                Product.builder().id("3").name("P3").type("CELLPHONES").price(price(300.0)).build()
        );

        when(repository.findAll()).thenReturn(products);

        ProductFilter filter = ProductFilter.builder()
                .pageSize(2)
                .build();
        List<Product> result = productService.searchProducts(filter);

        assertEquals(2, result.size());
        assertEquals("P1", result.get(0).getName());
        assertEquals("P2", result.get(1).getName());
    }

    @Test
    @DisplayName("Should treat zero or negative page as page 1")
    void testSearchProductsWithInvalidPage() {
        List<Product> products = Arrays.asList(
                Product.builder().id("1").name("P1").type("CELLPHONES").price(price(100.0)).build(),
                Product.builder().id("2").name("P2").type("CELLPHONES").price(price(200.0)).build()
        );

        when(repository.findAll()).thenReturn(products);

        ProductFilter filterZero = ProductFilter.builder()
                .page(0)
                .pageSize(2)
                .build();
        List<Product> resultZero = productService.searchProducts(filterZero);
        assertEquals(2, resultZero.size());
        assertEquals("P1", resultZero.get(0).getName());

        ProductFilter filterNegative = ProductFilter.builder()
                .page(-1)
                .pageSize(2)
                .build();
        List<Product> resultNegative = productService.searchProducts(filterNegative);
        assertEquals(2, resultNegative.size());
        assertEquals("P1", resultNegative.get(0).getName());
    }

    @Test
    @DisplayName("Should apply default units from template when unit is null")
    void testApplyDefaultUnits() {
        mockValidType("CELLPHONES");

        Product product = Product.builder()
                .name("Produto Sem Unit")
                .price(MeasurableValue.builder().value(99.99).build()) // No unit
                .weight(MeasurableValue.builder().value(0.5).build())  // No unit
                .type("CELLPHONES")
                .build();

        when(repository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.createProduct(product);

        assertEquals("BRL", result.getPrice().getUnit());
        assertEquals("kg", result.getWeight().getUnit());
    }

    @Test
    @DisplayName("Should throw exception for unknown specification key")
    void testCreateProductWithUnknownSpecKey() {
        mockValidTypeWithSpecs("CELLPHONES", Map.of(
                "brand", FieldDefinition.builder().type("text").required(false).comparable(false).build(),
                "storage_gb", FieldDefinition.builder().type("number").defaultUnit("GB").required(false).comparable(true).build()
        ));

        Product product = Product.builder()
                .name("iPhone")
                .price(price(999.99))
                .type("CELLPHONES")
                .specifications(new HashMap<>(Map.of("unknown_key", "value")))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(product);
        });

        assertTrue(exception.getMessage().contains("Unknown specification key"));
    }

    @Test
    @DisplayName("Should throw exception when required spec is missing")
    void testCreateProductWithMissingRequiredSpec() {
        mockValidTypeWithSpecs("CELLPHONES", Map.of(
                "brand", FieldDefinition.builder().type("text").required(true).comparable(false).build()
        ));

        Product product = Product.builder()
                .name("iPhone")
                .price(price(999.99))
                .type("CELLPHONES")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(product);
        });

        assertTrue(exception.getMessage().contains("Specification 'brand' is required"));
    }

    @Test
    @DisplayName("Should throw exception when required field (size) is missing")
    void testCreateProductWithMissingRequiredField() {
        mockValidTypeWithRequiredSize("CELLPHONES");

        Product product = Product.builder()
                .name("iPhone")
                .price(price(999.99))
                .type("CELLPHONES")
                .specifications(new HashMap<>(Map.of("brand", "Apple")))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(product);
        });

        assertTrue(exception.getMessage().contains("Field 'size' is required"));
    }

    @Test
    @DisplayName("Should normalize type to UPPERCASE when creating product")
    void testCreateProductNormalizesTypeToUppercase() {
        // Mock both lowercase and uppercase since service checks isValidType before normalization
        lenient().when(templateService.isValidType("clothing")).thenReturn(true);
        lenient().when(templateService.getTemplate("clothing")).thenReturn(Optional.of(
                ProductTemplate.builder()
                        .name("CLOTHING")
                        .displayName("Roupas")
                        .fields(Map.of(
                                "price", FieldDefinition.builder().type("number").defaultUnit("BRL").required(true).comparable(true).build(),
                                "size", FieldDefinition.builder().type("text").required(false).comparable(false).build(),
                                "weight", FieldDefinition.builder().type("number").defaultUnit("kg").required(false).comparable(true).build()
                        ))
                        .specifications(Map.of())
                        .build()
        ));

        Product product = Product.builder()
                .name("T-Shirt")
                .price(price(50.0))
                .type("clothing")
                .build();

        when(repository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.createProduct(product);

        assertEquals("CLOTHING", result.getType());
    }

    @Test
    @DisplayName("Should throw exception when spec of type number is not numeric")
    void testCreateProductWithInvalidNumericSpec() {
        mockValidTypeWithSpecs("CELLPHONES", Map.of(
                "storage_gb", FieldDefinition.builder().type("number").defaultUnit("GB").required(false).comparable(true).build()
        ));

        Product product = Product.builder()
                .name("iPhone")
                .price(price(999.99))
                .type("CELLPHONES")
                .specifications(new HashMap<>(Map.of("storage_gb", "not-a-number")))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(product);
        });

        assertTrue(exception.getMessage().contains("must be a valid number"));
    }

    @Test
    @DisplayName("Should throw exception when spec of type date has invalid format")
    void testCreateProductWithInvalidDateSpec() {
        mockValidTypeWithSpecs("FOOD", Map.of(
                "expiration_date", FieldDefinition.builder().type("date").required(false).comparable(false).build()
        ));

        Product product = Product.builder()
                .name("Coffee")
                .price(price(45.0))
                .type("FOOD")
                .specifications(new HashMap<>(Map.of("expiration_date", "invalid-date")))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(product);
        });

        assertTrue(exception.getMessage().contains("must be a valid date"));
    }

}
